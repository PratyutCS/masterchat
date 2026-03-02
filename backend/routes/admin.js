const express = require('express');
const router = express.Router();
const User = require('../models/User');
const Conversation = require('../models/Conversation');
const Message = require('../models/Message');
const ReadWatermark = require('../models/ReadWatermark');

/**
 * GET /api/admin/stats
 * Quick summary of the system
 */
router.get('/stats', async (req, res) => {
    try {
        const userCount = await User.countDocuments();
        const conversationCount = await Conversation.countDocuments();
        const messageCount = await Message.countDocuments();

        res.json({
            users: userCount,
            conversations: conversationCount,
            messages: messageCount,
            uptime: process.uptime(),
            timestamp: new Date()
        });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

/**
 * GET /api/admin/all-data
 * Full dump for the debug panel (not for production use!)
 */
router.get('/all-data', async (req, res) => {
    try {
        const users = await User.find().select('-password').sort({ createdAt: -1 }).lean();
        const conversations = await Conversation.find().populate('members', 'username email').sort({ updatedAt: -1 }).lean();

        // Fetch last 1000 messages across all conversations, newest first
        const messages = await Message.find()
            .sort({ createdAt: -1 })
            .limit(1000)
            .lean();

        // Cleanup orphaned watermarks (belonging to deleted conversations)
        const activeConvIds = await Conversation.distinct('_id');
        await ReadWatermark.deleteMany({ conversationId: { $nin: activeConvIds } });

        const watermarks = await ReadWatermark.find().lean();

        res.json({
            users,
            conversations,
            messages,
            watermarks
        });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

/**
 * DELETE /api/admin/:type/:id
 * Row deletion for the admin panel
 */
router.delete('/:type/:id', async (req, res) => {
    try {
        const { type, id } = req.params;
        let result;

        const io = req.app.get('io');

        switch (type) {
            case 'users':
                result = await User.deleteOne({ _id: id });
                if (io) io.emit('global_sync_required', { reason: 'user_deleted', id });
                break;
            case 'conversations':
                const conv = await Conversation.findById(id);
                if (conv) {
                    const memberIds = conv.members;
                    await Message.deleteMany({ conversationId: id });
                    await ReadWatermark.deleteMany({ conversationId: id });
                    result = await Conversation.deleteOne({ _id: id });

                    if (io) {
                        io.to(id).emit('conversation_deleted', { conversationId: id });
                        memberIds.forEach(mId => {
                            io.to(`user_${mId.toString()}`).emit('global_sync_required', { reason: 'conversation_deleted', conversationId: id });
                        });
                    }
                }
                break;
            case 'messages':
                const msg = await Message.findById(id);
                if (msg) {
                    result = await Message.deleteOne({ _id: id });
                    if (io) {
                        io.to(msg.conversationId.toString()).emit('message_deleted_from_admin', {
                            messageId: id,
                            conversationId: msg.conversationId
                        });
                    }
                }
                break;
            case 'watermarks':
                result = await ReadWatermark.deleteOne({ _id: id });
                break;
            default:
                return res.status(400).json({ error: 'Invalid type' });
        }

        res.json({ success: true, deletedCount: result.deletedCount });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

module.exports = router;
