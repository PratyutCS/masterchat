const mongoose = require('mongoose');

/**
 * Message Schema (Merged with Offline-First architecture)
 * 
 * Fields:
 *   - _id:            Client-generated UUID to ensure idempotency
 *   - conversationId: Reference to the Conversation this message belongs to
 *   - senderId:       Reference to the User who sent the message
 *   - receiverId:     Reference to the receiving User
 *   - text:           The original text string
 *   - content:        Additional message content
 *   - status:         Delivery status
 *   - createdAt:      Auto-set by Mongoose timestamps
 *   - updatedAt:      Auto-set by Mongoose timestamps
 */
const messageSchema = new mongoose.Schema(
    {
        _id: {
            type: String,
            required: true,
        },
        conversationId: {
            type: mongoose.Schema.Types.ObjectId,
            ref: 'Conversation',
            required: true,
            index: true,
        },
        msgUuid: {
            type: String,
            required: true,
            unique: true,
            index: true,
        },
        sequenceId: {
            type: Number,
            required: false,
        },
        senderId: {
            type: mongoose.Schema.Types.ObjectId,
            ref: 'User',
            required: true,
        },
        receiverId: {
            type: mongoose.Schema.Types.ObjectId,
            ref: 'User',
        },
        text: {
            type: String,
            required: true,
            maxlength: 5000,
        },
        content: {
            type: String,
            maxlength: 5000,
        },
        status: {
            type: String,
            enum: ['sent', 'delivered', 'read'],
            default: 'sent',
            lowercase: true
        },
        clientTimestamp: {
            type: Date,
            required: true,
        },
        sentAt: {
            type: Date,
        },
        deliveredAt: {
            type: Date,
        },
        seenAt: {
            type: Date,
        },
        readAt: {
            type: Date,
        },
        isRead: {
            type: Boolean,
            default: false,
        }
    },
    {
        timestamps: true,
    }
);

// Compound index for fast and sorted pagination per conversation
messageSchema.index({ conversationId: 1, sequenceId: 1 });

// Indexed for fast incremental pull using updatedAt
messageSchema.index({ updatedAt: 1 });

module.exports = mongoose.model('Message', messageSchema);
