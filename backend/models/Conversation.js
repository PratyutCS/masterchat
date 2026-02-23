const mongoose = require('mongoose');

/**
 * Conversation Schema (private 1-to-1 only)
 * 
 * Fields:
 *   - members:   Array of exactly 2 User ObjectIds
 *   - createdAt: Auto-set by Mongoose timestamps
 */
const conversationSchema = new mongoose.Schema(
    {
        members: [
            {
                type: mongoose.Schema.Types.ObjectId,
                ref: 'User',
                required: true,
            },
        ],
        lastSequenceId: {
            type: Number,
            default: 0
        }
    },
    {
        timestamps: true,
    }
);

module.exports = mongoose.model('Conversation', conversationSchema);
