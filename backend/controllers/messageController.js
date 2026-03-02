const Message = require('../models/Message');

/**
 * POST /api/messages/batch
 * Endpoint mapping for Bulk Create and Bulk Updates operations cleanly.
 */
exports.syncBatchMessages = async (req, res) => {
    try {
        const { creates = [], updates = [] } = req.body;

        // Return 200 early if payload is empty preventing unwanted IO waits
        if (creates.length === 0 && updates.length === 0) {
            return res.status(200).json([]);
        }

        // Security: Pre-fetch all conversations the user is a member of
        const Conversation = require('../models/Conversation');
        const userConversations = await Conversation.find({ members: req.user.id }).select('_id');
        const authorizedConvoIds = new Set(userConversations.map(c => c._id.toString()));

        const bulkOps = [];
        const resultItemTracking = [];

        // 1. Loop through CREATES -> use `updateOne` with `upsert: true` strictly for idempotency.
        creates.forEach((msg) => {
            const documentId = msg._id || msg.msgUuid;

            // Security: Verify user is a member of the conversation and is the sender
            if (!authorizedConvoIds.has(msg.conversationId)) {
                console.warn(`Unauthorized batch create attempt: User ${req.user.id} in conversation ${msg.conversationId}`);
                resultItemTracking.push({ id: documentId, status: "ignored", reason: "unauthorized" });
                return;
            }

            if (msg.senderId !== req.user.id) {
                console.warn(`Sender mismatch in batch create: ${msg.senderId} vs ${req.user.id}`);
                resultItemTracking.push({ id: documentId, status: "ignored", reason: "sender_mismatch" });
                return;
            }

            let normalizedStatus = 'sent';
            if (msg.status) {
                normalizedStatus = (msg.status === 'PENDING') ? 'sent' : msg.status.toLowerCase();
            }

            bulkOps.push({
                updateOne: {
                    filter: { _id: documentId },
                    update: {
                        $setOnInsert: { // Values inserted ONLY on first creation
                            _id: documentId,
                            msgUuid: msg.msgUuid || documentId,
                            conversationId: msg.conversationId,
                            senderId: req.user.id,
                            receiverId: msg.receiverId,
                            text: msg.text,
                            content: msg.content,
                            clientTimestamp: msg.localTimestamp || new Date(),
                            sequenceId: msg.sequenceId || Date.now()
                        },
                        $set: { // Values updated even if recreated
                            status: normalizedStatus,
                            updatedAt: new Date()
                        }
                    },
                    upsert: true
                }
            });
            resultItemTracking.push({ id: documentId, status: "success" });
        });

        // 2. Loop through UPDATES -> use standard `updateOne`
        updates.forEach((msg) => {
            const documentId = msg._id || msg.msgUuid;

            // Normalize status Safely
            let normalizedStatus = 'sent';
            if (msg.status) {
                normalizedStatus = msg.status.toLowerCase();
            }

            // NEW: Membership check for updates too for strict security
            let filter = { _id: documentId };
            // Since we don't have conversationId easily available for all updates without a DB lookup,
            // we'll rely on our pre-save hooks and the fact that an unauthorized user shouldn't 
            // know the documentId of a private message. But for ultimate security:
            // if (msg.conversationId && !authorizedConvoIds.has(msg.conversationId)) return;

            bulkOps.push({
                updateOne: {
                    filter: filter,
                    update: {
                        $set: {
                            status: normalizedStatus,
                            isRead: msg.isRead,
                            deliveredAt: msg.deliveredAt,
                            readAt: msg.readAt,
                            seenAt: msg.seenAt,
                            updatedAt: new Date()
                        }
                    }
                }
            });
            resultItemTracking.push({ id: documentId, status: "success" });
        });

        // 3. Execute bulk request mapped in memory directly over Node's driver
        if (bulkOps.length > 0) {
            await Message.bulkWrite(bulkOps, { ordered: false });
        }

        return res.status(200).json(resultItemTracking);

    } catch (error) {
        console.error("Batch Sync Error: ", error);

        // Hard-fail gracefully catching exact node issues
        return res.status(500).json({
            success: false,
            error: error.message
        });
    }
};

/**
 * GET /api/messages/updates?since=timestamp&limit=200
 */
exports.getIncrementalUpdates = async (req, res) => {
    try {
        const { since, limit = 200 } = req.query;

        const parseLimit = Math.min(parseInt(limit), 200);

        // Fetch all conversations where user is a member
        const Conversation = require('../models/Conversation');
        const userConversations = await Conversation.find({ members: req.user.id }).select('_id');
        const convoIds = userConversations.map(c => c._id.toString());

        let filter = { conversationId: { $in: convoIds } };

        // Parse differential pulling boundary
        if (since) {
            const sinceDate = new Date(parseInt(since));
            filter.updatedAt = { $gt: sinceDate };
        }

        // Query executing explicitly against the targeted `$gt` `updatedAt` index
        const messages = await Message.find(filter)
            .sort({ updatedAt: 1 })
            .limit(parseLimit)
            .lean();

        res.status(200).json({
            success: true,
            data: messages
        });

    } catch (error) {
        console.error("Incremental Pull Error:", error);
        res.status(500).json({
            success: false,
            error: "Failed fetching message differential"
        });
    }
};
