const express = require('express');
const router = express.Router();
const mongoose = require('mongoose');
const auth = require('../middleware/auth');
const Message = require('../models/Message');
const Conversation = require('../models/Conversation');

// Dependency injection workaround for Socket.IO (will be passed from server.js if needed, or we can require global io)
// For simplicity, we assume `req.app.get('io')` is accessible to emit events.

/**
 * UPSTREAM SYNC: Batch send messages
 * POST /api/sync/messages
 * Payload: [ { msgUuid, conversationId, text, clientTimestamp }, ... ]
 */
router.post('/messages', auth, async (req, res) => {
    try {
        const messages = req.body; // Array of pending message objects
        if (!Array.isArray(messages) || messages.length === 0) {
            return res.status(400).json({ msg: 'Expected a non-empty array of messages' });
        }

        const io = req.app.get('io');
        const results = [];

        // Process messages sequentially to assign monotonic sequence IDs per conversation.
        // In a highly concurrent MongoDB system, we must lock or use atomic operations.
        for (const msg of messages) {
            const { msgUuid, conversationId, text, clientTimestamp } = msg;

            // 1. Check if message already exists (idempotency key)
            const existingMsg = await Message.findOne({ msgUuid });
            if (existingMsg) {
                results.push(existingMsg);
                continue; // Already processed, return it
            }

            // 2. Atomically increment the sequence ID for this conversation
            const conversation = await Conversation.findOneAndUpdate(
                { _id: conversationId, members: req.user.id },
                {
                    $inc: { lastSequenceId: 1 },
                    $set: { updatedAt: new Date() }
                },
                { new: true } // Return updated doc
            );

            if (!conversation) {
                // Ignore if user isn't part of convo
                continue;
            }

            // 3. Create and save the message
            const newMsg = new Message({
                conversationId,
                msgUuid,
                sequenceId: conversation.lastSequenceId,
                senderId: req.user.id,
                text,
                clientTimestamp: clientTimestamp || new Date(),
                sentAt: new Date(),
                status: 'sent'
            });

            await newMsg.save();
            results.push(newMsg);

            // 4. Emit silent push / socket event to room
            if (io) {
                // Notify the conversation room (for users currently IN the chat)
                const room = conversationId.toString();
                io.to(room).emit('new_message_available', {
                    conversationId,
                    sequenceId: newMsg.sequenceId
                });

                // Notify individual members (for users on the Dashboard/Conversation List)
                const recipients = conversation.members.filter(m => m.toString() !== req.user.id);
                recipients.forEach(recipientId => {
                    const roomName = `user_${recipientId.toString()}`;
                    console.log(`Pushed global notification to recipient: ${roomName}`);
                    io.to(roomName).emit('new_message_available', {
                        conversationId: conversationId.toString(),
                        sequenceId: newMsg.sequenceId
                    });
                });
            }
        }

        // Return authoritative state back to the sending client
        res.json(results);
    } catch (err) {
        console.error('Error in batch sync upstream:', err.message);
        res.status(500).send('Server error');
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
        const { conversationId, afterSequenceId } = req.query;

        if (!conversationId) {
            return res.status(400).json({ msg: 'conversationId required' });
        }

        const seqId = parseInt(afterSequenceId, 10) || 0;

        // Ensure user is part of the conversation
        const conversation = await Conversation.findOne({
            _id: conversationId,
            members: req.user.id
        });

        if (!conversation) {
            return res.status(404).json({ msg: 'Conversation not found' });
        }

        // Fetch messages strictly greater than afterSequenceId
        const messages = await Message.find({
            conversationId,
            sequenceId: { $gt: seqId }
        })
            .sort({ sequenceId: 1 })
            .limit(200); // Batch limit

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
                status: 'sent'
            },
            { $set: { status: 'delivered' } }
        );

        // If we updated any messages, notify the sender
        if (deliveredResult.modifiedCount > 0) {
            const io = req.app.get('io');
            if (io) {
                const lastMsg = await Message.findOne({ conversationId })
                    .sort({ sequenceId: -1 });

                // Notify sender's personal room (dashboard updates)
                const senderRoom = conversation.members.find(m => m.toString() !== req.user.id);
                if (senderRoom) {
                    io.to(`user_${senderRoom.toString()}`).emit('message_delivered', {
                        conversationId: conversationId.toString(),
                        sequenceId: lastMsg ? lastMsg.sequenceId : 0
                    });
                }

                // Notify conversation room (in-chat updates)
                io.to(conversationId.toString()).emit('message_delivered', {
                    conversationId: conversationId.toString(),
                    sequenceId: lastMsg ? lastMsg.sequenceId : 0
                });
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
            const ReadWatermark = require('../models/ReadWatermark');
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
                { $set: { status: 'read', readAt: new Date() } }
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

        const ReadWatermark = require('../models/ReadWatermark');

        // Fetch all watermarks for this conversation (for both users)
        const watermarks = await ReadWatermark.find({ conversationId });

        res.json(watermarks);
    } catch (err) {
        console.error('Error fetching watermarks:', err.message);
        res.status(500).send('Server error');
    }
});

module.exports = router;
