const mongoose = require('mongoose');

/**
 * Read Watermark — per-user, per-conversation read state.
 *
 * Stores the highest sequenceId the user has read.
 * Monotonic: readUpToSeq only increases via MAX().
 * Compound unique index on (conversationId, userId).
 */
const readWatermarkSchema = new mongoose.Schema(
    {
        conversationId: {
            type: mongoose.Schema.Types.ObjectId,
            ref: 'Conversation',
            required: true,
        },
        userId: {
            type: mongoose.Schema.Types.ObjectId,
            ref: 'User',
            required: true,
        },
        readUpToSeq: {
            type: Number,
            default: 0,
        },
    },
    {
        timestamps: true,
    }
);

readWatermarkSchema.index({ conversationId: 1, userId: 1 }, { unique: true });

module.exports = mongoose.model('ReadWatermark', readWatermarkSchema);
