const jwt = require('jsonwebtoken');
const Message = require('../models/Message');
const User = require('../models/User');
const Conversation = require('../models/Conversation');

/**
 * Socket.IO Event Handler
 * 
 * Manages real-time communication:
 *   - authenticate:      Client sends JWT to verify identity
 *   - join_conversation:  Client joins a conversation room
 *   - send_message:       Client sends a message to a conversation
 *   - mark_read:          Client marks messages as read
 *   - disconnect:         Cleanup on disconnect
 * 
 * Uses a simple Map to track userId -> socketId for online status.
 */

// Map of userId (string) -> socketId for tracking online users
const onlineUsers = new Map();

function setupSocket(io) {
    io.on('connection', (socket) => {
        console.log(`Socket connected: ${socket.id}`);

        /**
         * Event: authenticate
         * Payload: { token: string }
         * 
         * Verifies the JWT and associates this socket with a user.
         * Broadcasts user_online to all other connected users.
         */
        socket.on('authenticate', async (data) => {
            try {
                const { token } = data;
                if (!token) {
                    socket.emit('auth_error', { error: 'Token required' });
                    return;
                }

                // Verify JWT
                const decoded = jwt.verify(token, process.env.JWT_SECRET);
                const userId = decoded.id;
                const username = decoded.username;

                // Store user info on the socket object
                socket.userId = userId;
                socket.username = username;

                // Track online status
                onlineUsers.set(userId, socket.id);

                // Join user-specific room for global notifications
                socket.join(`user_${userId}`);

                // Update lastSeen
                await User.findByIdAndUpdate(userId, { lastSeen: new Date() });

                // Notify all other users that this user is online
                socket.broadcast.emit('user_online', {
                    userId,
                    username,
                });

                // Send back authentication success
                socket.emit('authenticated', { userId, username });

                console.log(`User authenticated: ${username} (${userId})`);
            } catch (error) {
                console.error('Socket auth error:', error.message);
                socket.emit('auth_error', { error: 'Authentication failed' });
            }
        });

        /**
         * Event: join_conversation
         * Payload: { conversationId: string }
         * 
         * Joins the socket to a Socket.IO room named by the conversationId.
         * This allows messages to be broadcast only to participants.
         */
        socket.on('join_conversation', async (data) => {
            try {
                const { conversationId } = data;
                if (!socket.userId) {
                    socket.emit('auth_error', { error: 'Not authenticated' });
                    return;
                }

                // Verify user is a member of this conversation
                const conversation = await Conversation.findById(conversationId);
                if (!conversation) {
                    socket.emit('error_message', { error: 'Conversation not found' });
                    return;
                }

                const isMember = conversation.members.some(
                    (memberId) => memberId.toString() === socket.userId
                );
                if (!isMember) {
                    socket.emit('error_message', { error: 'Not a member of this conversation' });
                    return;
                }

                // Join the room
                socket.join(conversationId);
                console.log(`User ${socket.username} joined room: ${conversationId}`);
            } catch (error) {
                console.error('Join conversation error:', error.message);
                socket.emit('error_message', { error: 'Failed to join conversation' });
            }
        });

        /**
         * Event: typing_start
         * Payload: { conversationId: string }
         * 
         * Broadcasts typing status to the room.
         */
        socket.on('typing_start', (data) => {
            try {
                const { conversationId } = data;
                if (!socket.userId || !conversationId) return;

                // Broadcast to others in the room
                socket.to(conversationId).emit('typing_start', {
                    conversationId,
                    userId: socket.userId,
                    username: socket.username
                });
            } catch (error) {
                console.error('Typing start error:', error.message);
            }
        });

        /**
         * Event: typing_stop
         * Payload: { conversationId: string }
         * 
         * Broadcasts typing stop status to the room.
         */
        socket.on('typing_stop', (data) => {
            try {
                const { conversationId } = data;
                if (!socket.userId || !conversationId) return;

                // Broadcast to others in the room
                socket.to(conversationId).emit('typing_stop', {
                    conversationId,
                    userId: socket.userId
                });
            } catch (error) {
                console.error('Typing stop error:', error.message);
            }
        });

        /**
         * Event: mark_read
         * Payload: { conversationId: string, messageIds: [string], sequenceId: number }
         * 
         * Marks messages as read.
         */
        socket.on('mark_read', async (data) => {
            try {
                const { conversationId, messageIds, sequenceId } = data;
                if (!socket.userId) {
                    socket.emit('auth_error', { error: 'Not authenticated' });
                    return;
                }

                if (!conversationId) return;

                // If sequenceId is provided, mark all messages up to that sequenceId as read
                let query = {
                    conversationId,
                    senderId: { $ne: socket.userId }, // only mark OTHER user's messages
                };

                if (sequenceId !== undefined) {
                    query.sequenceId = { $lte: sequenceId };
                } else if (messageIds && messageIds.length) {
                    query.msgUuid = { $in: messageIds };
                } else {
                    return; // nothing to mark
                }

                // Monotonic progression: sent/delivered -> read
                query.status = { $ne: 'read' };

                // Upsert ReadWatermark if sequenceId is provided
                if (sequenceId !== undefined) {
                    const ReadWatermark = require('../models/ReadWatermark');
                    await ReadWatermark.findOneAndUpdate(
                        { conversationId, userId: socket.userId },
                        { $max: { readUpToSeq: sequenceId } },
                        { upsert: true, new: true }
                    );
                }

                // Update messages to 'read'
                await Message.updateMany(query, { status: 'read', readAt: new Date() });

                // Notify the other user in the room (for those currently IN the chat)
                socket.to(conversationId).emit('message_read', {
                    conversationId,
                    messageIds,
                    sequenceId,
                    readBy: socket.userId,
                });

                // Notify specifically for Dashboard updates (so grey ticks turn blue globally)
                const conv = await Conversation.findById(conversationId);
                if (conv) {
                    conv.members.forEach(memberId => {
                        const mIdStr = memberId.toString();
                        if (mIdStr !== socket.userId) {
                            const personalRoom = `user_${mIdStr}`;
                            console.log(`Broadcasting read status to personal room: ${personalRoom}`);
                            io.to(personalRoom).emit('message_read', {
                                conversationId,
                                sequenceId,
                                readBy: socket.userId,
                            });
                        }
                    });
                }

                console.log(`Messages marked read by ${socket.username} in ${conversationId}`);
            } catch (error) {
                console.error('Mark read error:', error.message);
            }
        });

        /**
         * Event: disconnect
         * 
         * Cleanup: update lastSeen, remove from onlineUsers, notify others.
         */
        socket.on('disconnect', async () => {
            if (socket.userId) {
                // Remove from online tracking
                onlineUsers.delete(socket.userId);

                // Wait a brief moment to see if they reconnect (prevent flicker)? 
                // For simplicity, just update and broadcast immediately.
                await User.findByIdAndUpdate(socket.userId, { lastSeen: new Date() });

                // Notify all users that this user went offline
                socket.broadcast.emit('user_offline', {
                    userId: socket.userId,
                    username: socket.username,
                    lastSeen: new Date(),
                });

                console.log(`User disconnected: ${socket.username}`);
            } else {
                console.log(`Socket disconnected: ${socket.id} (unauthenticated)`);
            }
        });
    });
}

module.exports = setupSocket;
