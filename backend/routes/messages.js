const express = require('express');
const Message = require('../models/Message');
const Conversation = require('../models/Conversation');
const auth = require('../middleware/auth');
const messageController = require('../controllers/messageController');

const router = express.Router();

// Register batch and pull endpoints *before* the /:conversationId parameter route
router.post('/batch', auth, messageController.syncBatchMessages);
router.get('/updates', auth, messageController.getIncrementalUpdates);

/**
 * GET /api/messages/:conversationId
 * 
 * Returns messages for a conversation, paginated.
 * Query params:
 *   - page (default: 1)
 *   - limit (default: 50)
 * 
 * Messages are returned oldest-first so the client can append them.
 * Protected route — requires JWT.
 */
router.get('/:conversationId', auth, async (req, res) => {
    try {
        const { conversationId } = req.params;
        const page = parseInt(req.query.page) || 1;
        const limit = parseInt(req.query.limit) || 50;

        // Verify the user is a member of this conversation
        const conversation = await Conversation.findById(conversationId);
        if (!conversation) {
            return res.status(404).json({ error: 'Conversation not found' });
        }

        const isMember = conversation.members.some(
            (memberId) => memberId.toString() === req.user.id
        );
        if (!isMember) {
            return res.status(403).json({ error: 'Not a member of this conversation' });
        }

        // Fetch messages with pagination (newest first for skip/limit, then reverse)
        const totalMessages = await Message.countDocuments({ conversationId });
        const messages = await Message.find({ conversationId })
            .sort({ createdAt: -1 })          // newest first for pagination
            .skip((page - 1) * limit)
            .limit(limit)
            .populate('senderId', 'username')
            .lean();

        // Reverse so messages are oldest-first in the response
        messages.reverse();

        res.json({
            messages,
            page,
            totalPages: Math.ceil(totalMessages / limit),
            totalMessages,
        });
    } catch (error) {
        console.error('Get messages error:', error.message);
        res.status(500).json({ error: 'Server error' });
    }
});

module.exports = router;
