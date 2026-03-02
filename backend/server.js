/**
 * Chat Backend — Entry Point
 * 
 * Sets up Express server with:
 *   - JSON body parsing
 *   - CORS
 *   - REST API routes (/api/auth, /api/users, /api/conversations, /api/messages)
 *   - Socket.IO for real-time messaging
 *   - MongoDB connection via Mongoose
 */

require('dotenv').config();

const express = require('express');
const http = require('http');
const cors = require('cors');
const { Server } = require('socket.io');

const connectDB = require('./config/db');
const setupSocket = require('./socket/index');

// Route imports
const authRoutes = require('./routes/auth');
const userRoutes = require('./routes/users');
const conversationRoutes = require('./routes/conversations');
const messageRoutes = require('./routes/messages');
const syncRoutes = require('./routes/sync'); // Offline-first sync
const adminRoutes = require('./routes/admin'); // Data Inspection Panel

// Initialize Express app
const app = express();

// Create HTTP server (needed for Socket.IO)
const server = http.createServer(app);

// Initialize Socket.IO with CORS settings
const io = new Server(server, {
    cors: {
        origin: '*', // allow all origins for development
        methods: ['GET', 'POST'],
    },
});

// Make io accessible to routers
app.set('io', io);

// ----- Middleware -----
app.use(cors());
app.use(express.json());

// ----- REST API Routes -----
app.use('/api/auth', authRoutes);
app.use('/api/users', userRoutes);
app.use('/api/conversations', conversationRoutes);
app.use('/api/messages', messageRoutes);
app.use('/api/sync', syncRoutes);
app.use('/api/admin', adminRoutes);

// Health check & Admin Panel
app.get('/', (req, res) => {
    res.json({ message: 'Chat API is running' });
});

app.get('/admin', (req, res) => {
    res.sendFile(__dirname + '/public/admin.html');
});

// ----- Socket.IO Setup -----
setupSocket(io);

// ----- Start Server -----
const PORT = process.env.PORT || 5000;

connectDB().then(() => {
    server.listen(PORT, () => {
        console.log(`Server running on port ${PORT}`);
        console.log(`REST API: http://localhost:${PORT}/api`);
        console.log(`Socket.IO: ws://localhost:${PORT}`);
    });
});
