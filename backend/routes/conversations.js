const express = require('express');
const Conversation = require('../models/Conversation');
const Message = require('../models/Message');
const ReadWatermark = require('../models/ReadWatermark');
const auth = require('../middleware/auth');

const router = express.Router();

/**
 * POST /api/conversations
 * 
 * Body: { recipientId }
 * Creates a new 1-to-1 conversation, or returns the existing one
 * if these two users already have a conversation.
 * Protected route — requires JWT.
 */
router.post('/', auth, async (req, res) => {
    try {
        const { recipientId } = req.body;
        const senderId = req.user.id;

        if (!recipientId) {
            return res.status(400).json({ error: 'recipientId is required' });
        }

        // Don't allow conversation with self
        if (recipientId === senderId) {
            return res.status(400).json({ error: 'Cannot create conversation with yourself' });
        }

        // Check if conversation already exists between these two users
        let conversation = await Conversation.findOne({
            members: { $all: [senderId, recipientId] },
        }).populate('members', '-passwordHash');

        if (conversation) {
            return res.json(conversation);
        }

        // Create new conversation
        conversation = await Conversation.create({
            members: [senderId, recipientId],
        });

        // Populate members before returning
        conversation = await conversation.populate('members', '-passwordHash');

        res.status(201).json(conversation);
    } catch (error) {
        console.error('Create conversation error:', error.message);
        res.status(500).json({ error: 'Server error' });
    }
});

/**
 * GET /api/conversations
 * 
 * Returns all conversations for the currently authenticated user,
 * with members populated and sorted by most recent activity.
 * Protected route — requires JWT.
 */
router.get('/', auth, async (req, res) => {
    try {
        const conversations = await Conversation.find({
            members: req.user.id,
        })
            .populate('members', '-passwordHash')
            .sort({ updatedAt: -1 });

        // For each conversation, also fetch the last message (for preview)
        // and calculate the number of unread messages for the current user.
        const result = await Promise.all(
            conversations.map(async (conv) => {
                // Fetch last message
                const lastMessage = await Message.findOne({ conversationId: conv._id })
                    .populate('senderId', '-passwordHash')
                    .sort({ sequenceId: -1 })
                    .lean();

                // Calculate unread count: messages in this conversation
                // NOT sent by the current user, where status is NOT 'read'.
                const unreadCount = await Message.countDocuments({
                    conversationId: conv._id,
                    senderId: { $ne: req.user.id },
                    status: { $ne: 'read' }
                });

                return {
                    ...conv.toObject(),
                    lastMessage: lastMessage || null,
                    unreadCount: unreadCount,
                };
            })
        );

        res.json(result);
    } catch (error) {
        console.error('Get conversations error:', error.message);
        res.status(500).json({ error: 'Server error' });
    }
});

/**
 * DELETE /api/conversations/:id
 * 
 * Deletes a conversation and all associated messages.
 * Only members of the conversation can delete it.
 */
router.delete('/:id', auth, async (req, res) => {
    try {
        const conversationId = req.params.id;
        const userId = req.user.id;

        // Verify user is a member
        const conversation = await Conversation.findOne({
            _id: conversationId,
            members: userId
        });

        if (!conversation) {
            return res.status(404).json({ error: 'Conversation not found or not authorized' });
        }

        // 1. Get member IDs for notification before deletion
        const memberIds = conversation.members;

        // 2. Delete all associated messages and watermarks
        await Message.deleteMany({ conversationId });
        await ReadWatermark.deleteMany({ conversationId });

        // 3. Delete the conversation itself
        await Conversation.deleteOne({ _id: conversationId });

        // 4. Notify all participants via socket
        const io = req.app.get('io');
        if (io) {
            // Notify the room (for those currently in ChatActivity)
            io.to(conversationId).emit('conversation_deleted', { conversationId });

            // Notify each user's personal room (for those in Dashboard/ConversationList)
            memberIds.forEach(mId => {
                io.to(`user_${mId.toString()}`).emit('conversation_deleted', { conversationId });
            });
        }

        res.json({ message: 'Conversation and all associated messages deleted successfully' });
    } catch (error) {
        console.error('Delete conversation error:', error.message);
        res.status(500).json({ error: 'Server error' });
    }
});

module.exports = router;
