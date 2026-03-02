# Robust Two-Way Chat Persistence and Synchronization Architecture

This document describes a fault-tolerant, offline-first, and eventually consistent chat architecture capable of resolving common "failed to load chats" errors, lost messages, and misordering.

---

## 1. High-Level Objective

The chat system is fundamentally an **offline-first local database synchronized with a cloud source of truth**.

The core principles:
1. **Local Database as Source of Truth for the UI**: The application interface *only* renders data from the local database. It never blocks on the network to display a conversation.
2. **Server as Final Arbiter of Order**: The backend uniquely assigns strict monotonic sequence numbers to messages within a conversation, permanently establishing their topological order.
3. **Idempotency Everywhere**: Every operation (send, read receipt, sync) must be safely repeatable without risk of duplication or corruption.
4. **Clock-Ticking Consistency**: Continuous active synchronization ensure that client states are reconciled against the global source of truth every few seconds, catching edits, deletions, and status changes.

---

## 2. Local Device Storage (Client-Side)

### 2.1 Per-Conversation Local Cache
- **Storage Technology**: **SQLite via Room Database**. SQLite ensures ACID transactions locally and survives unexpected crashes.
- Each message acts as a single row in a `messages` table. 

### 2.2 Write Path (Optimistic Update)
When User A sends a message:
1. **Generate UUID (Client-Side)**: `msgUuid` (v4 UUID) guarantees global uniqueness. 
2. **Write to SQLite**: Insert into `messages` table with `status = PENDING`.
3. **Write to Sync Queue**: Insert a reference into a `sync_queue` table.
4. **Update UI**: The ViewModel observes the SQLite table instantly over LiveData.
5. **Trigger Sync Worker**: `LinearSyncWorker` processes the outbox and hits `POST /api/sync/messages`.

---

## 3. High-Frequency Heartbeat (The "Clock")

### 3.1 Recursive SyncScheduler
To overcome the 15-minute limitation of standard OS background jobs, MasterChat uses a **Recursive Heartbeat**:
- **Frequency**: **5 Seconds**.
- **Mechanism**: `SyncScheduler` enqueues a `OneTimeWorkRequest`. Upon completion, the worker schedules its next "tick" to fire in 5 seconds.
- **Independence**: This clock ticks regardless of user interaction or screen state.

### 3.2 StatusUpdateManager (Reactivity)
The client observes specialized socket events to trigger immediate actions:
- `global_sync_required`: Forces the clock to "tick" immediately.
- `message_deleted_from_admin`: Triggers an immediate local purge of a specific message UUID.

---

## 4. Bi-Directional Sync Mechanism

### 4.1 Upstream Sync (Outbox)
The `LinearSyncWorker` processes pending items in `sync_queue`. Once the server returns a `sequenceId`, the local row is updated and the queue item is cleared.

### 4.2 Downstream Sync (Dual-Pivot Pulling)
Instead of just pulling *new* messages, the system uses a dual-pivot approach:
1. **Sequence Pivot**: Pull messages where `sequenceId > lastPulledSeq`. (Catches new posts).
2. **Time Pivot**: Pull messages where `updatedAt > lastPulledAt`. (Catches edits, status changes, and server-side reflections).

### 4.3 Reconciliation Phase (Purge Sync)
After the pull phase, the worker validates the entire conversation's message IDs:
1. `GET /api/sync/reconcile-ids`: Client receives a full list of valid server-side UUIDs for the conversation.
2. **Local Purge**: Client executes `DELETE FROM messages WHERE conversationId = X AND msgUuid NOT IN (serverList)`. This ensures hard-deletions on the server (e.g., via Admin) are reflected offline.

---

## 5. Message Ordering & Integrity

- **Monotonic Sequence Numbers (`seq_id`)**: The backend assigns strictly ascending IDs per conversation.
- **Source of Truth for Sorting**: UI sorts `ORDER BY sequenceId ASC`.
- **Handling Pending Messages**: Pending messages (sequenceId = NULL) are sorted at the bottom using `localTimestamp`.
- **Watermark Persistence**: Read receipts are tracked via watermarks. The server tracks `readUpToSeq`, and the client applies this to its local database during every sync heartbeat.

---

## 6. Failure Modes & Resilience

- **Idempotency**: `msgUuid` is the primary key in MongoDB. Duplicate sync requests are safely ignored.
- **Fault Isolation**: Sync failures in Conversation A do not block the clock from processing Conversation B.
- **Dead-Letter Handling**: Orphaned or repeatedly failing items in the `sync_queue` are moved to a dead-letter state to prevent blocking the heartbeat.
- **Differential Acknowledgement**: The client explicitly tells the server the highest ID it has safely written to SQLite via `POST /api/sync/ack`.

---

## 7. Summary Sync Lifecycle (5-Second Cycle)

1. **Heartbeat Fires**: `LinearSyncWorker` awakens.
2. **Upstream Step**: Outbox messages are sent to `/api/sync/messages`.
3. **Pull Step**: Client requests incremental updates using `lastPulledSeq` AND `lastPulledAt`.
4. **Watermark Step**: Read receipts from other users are fetched and applied locally.
5. **Reconciliation Step**: Client prunes orphaned messages that no longer exist in the server's master list.
6. **Cycle Completion**: Worker schedules next tick via `SyncScheduler` in 5 seconds.
