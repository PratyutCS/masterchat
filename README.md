# MasterChat: High-Performance Offline-First Messaging Platform

MasterChat is a state-of-the-art, **Offline-First**, eventually consistent communication platform. It combines a robust REST synchronization engine with low-latency Socket.IO real-time events to provide a seamless "fast UI" experience across any network condition.

---

## 🚀 Key Features

*   **Atomic Offline-First Architecture**: Every action (send, read, delete) is persisted locally first for instant UI response and then synchronized in the background.
*   **Game-Engine Heartbeat Sync**: A high-frequency (5-second) background heartbeat ensures data parity even when Socket.IO events are missed.
*   **Full Data Reconciliation**: Identifies and prunes "zombie" local messages that have been hard-deleted on the server.
*   **Administrative Oversight**: A built-in Admin Panel for managing Users, Conversations, and Messages with real-time sync-triggering capabilities.
*   **Incremental Time-Based Pulling**: Optimized synchronization that catches message edits and status changes by tracking `updatedAt` timestamps.

---

## 🏗️ Project Structure

```
master_app/
├── backend/              # Node.js + Express + MongoDB + Socket.IO
│   ├── server.js         # Entry point & Socket.IO initialization
│   ├── routes/           # REST API Logic
│   │   ├── sync.js       # Core "Heavy Lifting" Synchronization Engine
│   │   ├── admin.js      # Administrative CRUD & Cascade Logic
│   │   └── auth/conv/msg # Standard endpoints
│   └── models/           # Mongoose schemas with sequenceId & updatedAt indexing
│
└── android/              # Android App (Java, MVVM, Room, WorkManager)
    └── app/src/main/java/com/masterapp/chat/
        ├── sync/         # The "Brain": Heartbeat (Scheduler) & Linear Sync Worker
        ├── local/        # Persistence: Room Entities & DAOs for offline safety
        ├── api/          # Network: Retrofit Sync & Reconciliation API
        ├── socket/       # Real-time: Centralized Socket.IO Manager
        └── ui/viewmodel  # Presentation: Reverse Layout + Reactivity
```

---

## 🛠️ Getting Started

### Backend Setup
1.  **Start MongoDB**: Ensure `mongod` is running on `localhost:27017`.
2.  **Environment**: Configure `.env` (JWT_SECRET, MONGO_URI).
3.  **Run**: `cd backend && npm install && node server.js`.
4.  **Admin Panel**: Access via `http://localhost:5000/admin`.

### Android Setup
1.  **Configure**: Update the server URL in `app/build.gradle` (use LAN IP for physical devices).
2.  **Run**: Open in Android Studio, sync Gradle, and install on a device/emulator.
3.  **Heartbeat**: The 5-second sync heartbeat starts automatically upon login.

---

## ⛓️ Synchronization Architecture

### Upstream (Client → Server)
Uses an **Outbox Pattern**. Messages are queued in Room and pushed by the `LinearSyncWorker` to `POST /api/sync/messages`. The server assigns a monotonic `sequenceId` to guarantee global ordering.

### Downstream (Server → Client)
Uses **Dual-Pivot Incremental Pulling**:
*   **Sequence-Based**: Pulls everything where `sequenceId > lastPulledSeq`.
*   **Time-Based**: Pulls everything where `updatedAt > lastPulledAt` (to catch edits).
*   **Reconciliation**: Compares local UUIDs against server active-sets to clear hard-deletions.

---

## 📡 Enhanced API & Socket Bus

### Core Sync Endpoints
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/sync/messages` | Batch-push local outbox messages |
| GET | `/api/sync/messages` | Incremental pull (afterSeq + updatedAfter) |
| GET | `/api/sync/reconcile-ids` | Full UUID set for local purging/reconciliation |
| POST | `/api/sync/ack` | Two-phase confirmation of message persistence |

### Admin & Control Bus
| Direction | Event | Payload | Description |
|-----------|-------|---------|-------------|
| Server→Client | `global_sync_required` | `{}` | Forces an immediate out-of-band sync |
| Server→Client | `message_deleted_from_admin` | `{ msgUuid }` | Direct local purge of a deleted message |
| Server→Client | `conversation_deleted` | `{ conversationId }` | Full local cleanup of a deleted chat |

---

## 📜 Design Principles
MasterChat strictly adheres to the **"Source of Truth Hierarchy"**:
1.  **UI Level**: Room is "God." The UI never waits for the network.
2.  **Sync Level**: MongoDB is "Final Arbiter." It resolves conflicts via server-side locking and sequence assignment.
3.  **Transport Level**: Socket.IO is the "Speed Layer," while REST + 5s Heartbeat is the "Reliability Layer."
