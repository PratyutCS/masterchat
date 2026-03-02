const express = require('express');
const router = express.Router();
const mongoose = require('mongoose');
const auth = require('../middleware/auth');
const Message = require('../models/Message');
const Conversation = require('../models/Conversation');
const ReadWatermark = require('../models/ReadWatermark');

// Dependency injection workaround for Socket.IO (will be passed from server.js if needed, or we can require global io)
// For simplicity, we assume `req.app.get('io')` is accessible to emit events.

/**
 * UPSTREAM SYNC: Batch send messages
 * POST /api/sync/messages
 * Payload: [ { msgUuid, conversationId, text, clientTimestamp }, ... ]
 */
router.post('/messages', auth, async (req, res) => {
    try {
        const messages = req.body;
        if (!Array.isArray(messages) || messages.length === 0) {
            return res.status(400).json({ error: 'Expected a non-empty array of messages' });
        }

        const io = req.app.get('io');
        const results = [];

        // 1. Group messages by conversationId for efficient sequence updates
        const groups = {};
        for (const msg of messages) {
            if (!msg.conversationId || !msg.msgUuid) continue;
            if (!groups[msg.conversationId]) groups[msg.conversationId] = [];
            groups[msg.conversationId].push(msg);
        }

        // 2. Process each conversation group atomically
        for (const convId of Object.keys(groups)) {
            const convMessages = groups[convId];
            const count = convMessages.length;

            // Atomically allocate a block of sequence IDs
            const conversation = await Conversation.findOneAndUpdate(
                { _id: convId, members: req.user.id },
                {
                    $inc: { lastSequenceId: count },
                    $set: { updatedAt: new Date() }
                },
                { new: true }
            );

            if (!conversation) {
                console.warn(`Sync warning: User ${req.user.id} attempted to sync to conversation ${convId} but is not a member or it doesn't exist.`);
                continue;
            }

            // The last assigned sequence ID on the server
            const baseSeq = conversation.lastSequenceId - count;
            const otherUser = conversation.members.find(m => m.toString() !== req.user.id);

            for (let i = 0; i < count; i++) {
                const msg = convMessages[i];
                const seq = baseSeq + i + 1;

                // Idempotency: skip if msgUuid exists
                const existing = await Message.findOne({ msgUuid: msg.msgUuid });
                if (existing) {
                    results.push(existing);
                    continue;
                }

                const newMsg = new Message({
                    _id: msg.msgUuid, // Explicitly set _id to the client UUID for schema consistency
                    msgUuid: msg.msgUuid,
                    conversationId: convId,
                    sequenceId: seq,
                    senderId: req.user.id,
                    receiverId: otherUser || null,
                    text: msg.text,
                    clientTimestamp: msg.clientTimestamp || new Date(),
                    sentAt: new Date(),
                    status: 'sent'
                });

                await newMsg.save();
                results.push(newMsg);

                // Fan out notification via Socket.IO
                if (io) {
                    // Update the generic "something changed" event
                    io.to(convId).emit('new_message_available', { conversationId: convId, sequenceId: seq });

                    // Specific dashboard update for recipients
                    if (otherUser) {
                        io.to(`user_${otherUser.toString()}`).emit('new_message_available', {
                            conversationId: convId,
                            sequenceId: seq
                        });
                    }
                }
            }
        }

        res.json(results);
    } catch (err) {
        console.error('Batch sync failure:', err);
        res.status(500).json({ error: 'Internal server error during sync' });
    }
});

/**
 * DOWNSTREAM SYNC: Pull messages (READ-ONLY — no side effects)
 * GET /api/sync/messages?conversationId=XYZ&afterSequenceId=123
 * 
 * Returns messages but does NOT mark them as delivered.
 * Client must call POST /api/sync/ack after persisting locally.
 */
router.get('/messages', auth, async (req, res) => {
    try {
        const { conversationId, afterSequenceId, updatedAfter } = req.query;

        if (!conversationId) {
            return res.status(400).json({ msg: 'conversationId required' });
        }

        const seqId = parseInt(afterSequenceId, 10) || 0;
        const lastUpdate = updatedAfter ? new Date(updatedAfter) : new Date(0);

        // Ensure user is part of the conversation
        const conversation = await Conversation.findOne({
            _id: conversationId,
            members: req.user.id
        });

        if (!conversation) {
            return res.status(404).json({ msg: 'Conversation not found' });
        }

        // Fetch messages that are either NEW (seqId) OR UPDATED (lastUpdate)
        const messages = await Message.find({
            conversationId,
            $or: [
                { sequenceId: { $gt: seqId } },
                { updatedAt: { $gt: lastUpdate } }
            ]
        })
            .sort({ sequenceId: 1 })
            .limit(200);

        res.json(messages);
    } catch (err) {
        console.error('Error in pull sync downstream:', err.message);
        res.status(500).send('Server error');
    }
});

/**
 * DELIVERY ACKNOWLEDGMENT: Client confirms local persistence
 * POST /api/sync/ack
 * Payload: { conversationId, maxSequenceId }
 * 
 * Called by the client AFTER it has successfully persisted pulled messages
 * into its local SQLite database. This two-phase approach prevents marking
 * messages as 'delivered' on the server when the client hasn't actually stored them.
 */
router.post('/ack', auth, async (req, res) => {
    try {
        const { conversationId, maxSequenceId } = req.body;

        if (!conversationId || maxSequenceId === undefined) {
            return res.status(400).json({ msg: 'conversationId and maxSequenceId required' });
        }

        // Ensure user is part of the conversation
        const conversation = await Conversation.findOne({
            _id: conversationId,
            members: req.user.id
        });

        if (!conversation) {
            return res.status(404).json({ msg: 'Conversation not found' });
        }

        // Mark messages as delivered (monotonic: only update 'sent' → 'delivered')
        const deliveredResult = await Message.updateMany(
            {
                conversationId,
                senderId: { $ne: req.user.id },
                sequenceId: { $lte: maxSequenceId },
                $or: [
                    { status: 'sent' },
                    { status: 'delivered', deliveredAt: null }
                ]
            },
            { $set: { status: 'delivered', deliveredAt: new Date() } }
        );

        // If we updated any messages, notify the sender
        if (deliveredResult.modifiedCount > 0) {
            const io = req.app.get('io');
            if (io) {
                // Determine who should receive the delivery update (the sender of the original messages)
                const originalSender = conversation.members.find(m => m.toString() !== req.user.id);
                if (originalSender) {
                    const payload = {
                        conversationId: conversationId.toString(),
                        sequenceId: maxSequenceId
                    };

                    // Notify sender's personal room (for dashboard view)
                    io.to(`user_${originalSender.toString()}`).emit('message_delivered', payload);

                    // Notify conversation room (for active chat view)
                    io.to(conversationId.toString()).emit('message_delivered', payload);
                }
            }
        }

        res.json({ acknowledged: deliveredResult.modifiedCount });
    } catch (err) {
        console.error('Error in delivery ack:', err.message);
        res.status(500).send('Server error');
    }
});

/**
 * READ ACKNOWLEDGMENT: Client confirms reading messages
 * POST /api/sync/read-ack
 * Payload: [ { conversationId, maxSequenceId }, ... ]
 *
 * Processes a batch of read watermarks. For each:
 * 1. Upserts the ReadWatermark (monotonic MAX)
 * 2. Updates Message statuses to 'read'
 * 3. Fans out 'message_read' socket events to sender
 */
router.post('/read-ack', auth, async (req, res) => {
    try {
        const readWatermarks = req.body;
        if (!Array.isArray(readWatermarks) || readWatermarks.length === 0) {
            return res.status(400).json({ msg: 'Expected array of read watermarks' });
        }

        const io = req.app.get('io');
        let totalAcked = 0;

        for (const item of readWatermarks) {
            const { conversationId, maxSequenceId } = item;

            // 1. Verify membership
            const conversation = await Conversation.findOne({
                _id: conversationId,
                members: req.user.id
            });

            if (!conversation) continue;

            // 2. Upsert ReadWatermark (Monotonic MAX)
            const watermark = await ReadWatermark.findOneAndUpdate(
                { conversationId, userId: req.user.id },
                { $max: { readUpToSeq: maxSequenceId } },
                { upsert: true, new: true }
            );

            // 3. Mark messages as read (idempotent, only where status != 'read' and sequenceId <= watermark)
            const readResult = await Message.updateMany(
                {
                    conversationId,
                    senderId: { $ne: req.user.id },
                    sequenceId: { $lte: watermark.readUpToSeq },
                    status: { $ne: 'read' }
                },
                { $set: { status: 'read', readAt: new Date(), deliveredAt: new Date() } }
            );

            // 4. If any messages were newly marked read, fanout via Socket.IO
            if (readResult.modifiedCount > 0 && io) {
                const senderId = conversation.members.find(m => m.toString() !== req.user.id);
                if (senderId) {
                    const payload = {
                        conversationId: conversationId.toString(),
                        sequenceId: watermark.readUpToSeq,
                        readBy: req.user.id
                    };

                    // Send to conversation room (fast path for users currently in the chat)
                    io.to(conversationId.toString()).emit('message_read', payload);

                    // Send to sender's personal room (for dashboard global ticks)
                    io.to(`user_${senderId.toString()}`).emit('message_read', payload);
                }
            }

            totalAcked++;
        }

        res.json({ acked: totalAcked });
    } catch (err) {
        console.error('Error in read ack batch sync:', err.message);
        res.status(500).send('Server error');
    }
});

/**
 * GET /api/sync/watermarks
 * 
 * Returns the read watermarks for a specific conversation.
 * Primarily used by offline users to catch up on what the other user has read.
 */
router.get('/watermarks', auth, async (req, res) => {
    try {
        const { conversationId } = req.query;

        if (!conversationId) {
            return res.status(400).json({ msg: 'conversationId required' });
        }

        // Ensure user is part of the conversation
        const conversation = await Conversation.findOne({
            _id: conversationId,
            members: req.user.id
        });

        if (!conversation) {
            return res.status(404).json({ msg: 'Conversation not found' });
        }

        // Fetch all watermarks for this conversation (for both users)
        const watermarks = await ReadWatermark.find({ conversationId });

        res.json(watermarks);
    } catch (err) {
        console.error('Error fetching watermarks:', err.message);
        res.status(500).send('Server error');
    }
});

/**
 * RECONCILIATION: Verify existence of messages
 * GET /api/sync/reconcile-ids?conversationId=XYZ
 * 
 * Returns all valid message UUIDs for a conversation.
 * Client uses this to delete local messages that were hard-deleted on server.
 */
router.get('/reconcile-ids', auth, async (req, res) => {
    try {
        const { conversationId } = req.query;
        if (!conversationId) return res.status(400).json({ error: 'conversationId required' });

        const conversation = await Conversation.findOne({ _id: conversationId, members: req.user.id });
        if (!conversation) return res.status(404).json({ error: 'Conversation not found' });

        const ids = await Message.find({ conversationId }).distinct('msgUuid');
        res.json({ msgUuids: ids });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

module.exports = router;
