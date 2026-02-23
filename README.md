# Phase 1 — Real-Time 1-to-1 Chat Application

A minimal real-time private chat app with a Node.js backend and Android frontend.

## Project Structure

```
master_app/
├── backend/          # Node.js + Express + MongoDB + Socket.IO
│   ├── server.js     # Entry point
│   ├── config/       # Database connection
│   ├── models/       # Mongoose schemas (User, Conversation, Message)
│   ├── middleware/    # JWT auth middleware
│   ├── routes/       # REST API routes
│   └── socket/       # Socket.IO event handlers
│
└── android/          # Android app (Java, MVVM, XML)
    └── app/src/main/
        └── java/com/masterapp/chat/
            ├── api/        # Retrofit interfaces + client
            ├── models/     # Data models
            ├── socket/     # Socket.IO manager
            ├── repository/ # Data repositories
            ├── viewmodel/  # MVVM ViewModels
            ├── ui/         # Activities + adapters
            └── util/       # Token manager, constants
```

---

## Backend Setup

### Prerequisites
- **Node.js** v18+ installed
- **MongoDB** running locally on default port (27017)

### Steps

```bash
# 1. Navigate to backend
cd backend

# 2. Install dependencies
npm install

# 3. Copy and configure environment variables
cp .env.example .env
# Edit .env if needed (defaults work for local dev)

# 4. Make sure MongoDB is running
# On Ubuntu/Debian:
sudo systemctl start mongod
# On macOS with Homebrew:
brew services start mongodb-community

# 5. Start the server
node server.js
```

You should see:
```
MongoDB connected: localhost
Server running on port 5000
REST API: http://localhost:5000/api
Socket.IO: ws://localhost:5000
```

### Test the API

```bash
# Register a user
curl -X POST http://localhost:5000/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","email":"alice@test.com","password":"password123"}'

# Login
curl -X POST http://localhost:5000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@test.com","password":"password123"}'
```

Both return: `{ "token": "eyJ...", "user": { ... } }`

---

## Android Setup

### Prerequisites
- **Android Studio** (latest stable)
- **Android SDK** API 34

### Steps

1. Open Android Studio
2. Select **Open an existing project**
3. Navigate to `master_app/android/` and open it
4. Wait for Gradle sync to complete
5. **Important:** Update the server URL in `app/build.gradle`:
   - For **emulator**: use `http://10.0.2.2:5000` (default, maps to host localhost)
   - For **physical device**: use your machine's LAN IP, e.g., `http://192.168.1.X:5000`
6. Build and run on a device/emulator

### Usage Flow

1. **Register** two different users (on two devices or with separate app data)
2. **Login** on each device
3. On one device, tap the **+** button to start a new chat
4. Select the other user from the list
5. Send messages — they appear in real-time on both devices!
6. Message status: ✓ = sent, ✓✓ = delivered/read

---

## API Endpoints

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/auth/register` | No | Register new user |
| POST | `/api/auth/login` | No | Login, get JWT |
| GET | `/api/users` | Yes | List all users |
| GET | `/api/users/:id` | Yes | Get user profile |
| POST | `/api/conversations` | Yes | Create/find conversation |
| GET | `/api/conversations` | Yes | List my conversations |
| GET | `/api/messages/:convId` | Yes | Get messages (paginated) |

## Socket Events

| Direction | Event | Payload |
|-----------|-------|---------|
| Client→Server | `authenticate` | `{ token }` |
| Client→Server | `join_conversation` | `{ conversationId }` |
| Client→Server | `send_message` | `{ conversationId, text }` |
| Client→Server | `mark_read` | `{ conversationId, messageIds[] }` |
| Server→Client | `authenticated` | `{ userId, username }` |
| Server→Client | `receive_message` | `{ message object }` |
| Server→Client | `message_delivered` | `{ messageId, conversationId }` |
| Server→Client | `message_read` | `{ conversationId, messageIds[], readBy }` |
| Server→Client | `user_online` | `{ userId, username }` |
| Server→Client | `user_offline` | `{ userId, username, lastSeen }` |
