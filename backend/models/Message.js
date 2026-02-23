const mongoose = require('mongoose');

/**
 * Message Schema
 * 
 * Fields:
 *   - conversationId: Reference to the Conversation this message belongs to
 *   - senderId:       Reference to the User who sent the message
 *   - text:           The message content
 *   - status:         Delivery status: 'sent', 'delivered', or 'read'
 *   - createdAt:      Auto-set by Mongoose timestamps
 */
const messageSchema = new mongoose.Schema(
    {
        conversationId: {
            type: mongoose.Schema.Types.ObjectId,
            ref: 'Conversation',
            required: true,
            index: true, // index for fast message lookups per conversation
        },
        msgUuid: {
            type: String,
            required: true,
            unique: true,
            index: true,
        },
        sequenceId: {
            type: Number,
            required: true,
        },
        senderId: {
            type: mongoose.Schema.Types.ObjectId,
            ref: 'User',
            required: true,
        },
        text: {
            type: String,
            required: true,
            maxlength: 5000,
        },
        status: {
            type: String,
            enum: ['sent', 'delivered', 'read'],
            default: 'sent',
        },
        clientTimestamp: {
            type: Date,
            required: true,
        },
        sentAt: {
            type: Date,
        },
        readAt: {
            type: Date,
        }
    },
    {
        timestamps: true,
    }
);

// Compound index for fast and sorted pagination per conversation
messageSchema.index({ conversationId: 1, sequenceId: 1 });

module.exports = mongoose.model('Message', messageSchema);
