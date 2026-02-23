# Robust Two-Way Chat Persistence and Synchronization Architecture

This document describes a fault-tolerant, offline-first, and eventually consistent chat architecture capable of resolving common "failed to load chats" errors, lost messages, and misordering.

---

## 1. High-Level Objective

The chat system is fundamentally an **offline-first local database synchronized with a cloud source of truth**.

The core principles:
1. **Local Database as Source of Truth for the UI**: The application interface *only* renders data from the local database. It never blocks on the network to display a conversation.
2. **Server as Final Arbiter of Order**: The backend uniquely assigns strict monotonic sequence numbers to messages within a conversation, permanently establishing their topological order.
3. **Idempotency Everywhere**: Every operation (send, read receipt, sync) must be safely repeatable without risk of duplication or corruption.

---

## 2. Local Device Storage (Client-Side)

### 2.1 Per-Conversation Local Cache
- **Storage Technology**: **SQLite**. On Android, use **Room Database**, and on iOS/Web/React Native use **WatermelonDB** or **GRDB**. SQLite ensures ACID transactions locally and survives unexpected crashes.
- Each message must act as a single row in a `messages` table. Do not store entire conversations as JSON blobs. 

### 2.2 Write Path (Optimistic Update)
When User A sends a message:
1. **Generate UUID (Client-Side)**: `msg_uuid` (e.g., `v4 UUID`) guarantees global uniqueness. 
2. **Write to SQLite**: Insert into `messages` table with `status = PENDING`, `local_timestamp = Date.now()`, and `server_seq_id = NULL`.
3. **Write to Sync Queue**: Insert a reference `(msg_uuid, operation = 'SEND')` into a `sync_queue` SQLite table.
4. **Update UI**: The ViewModel observes the SQLite table (using *LiveData/Flow* or *RxJava*) and instantly displays the pending message at the bottom.
5. **Trigger Sync Worker**: A background worker reads the `sync_queue` and attempts an HTTPS `POST /messages`.

### 2.3 Offline Behavior
If the device is offline, Steps 1-4 execute identically. Step 5 (Sync Worker) fails silently and registers a listener with the OS (e.g., Android `WorkManager` with `NetworkType.CONNECTED` constraint). When the network returns, the worker awakens and processes the `sync_queue` in FIFO order.

---

## 3. Cloud Backend Storage

### 3.1 Conversation Separation
- **Database Choice**: **PostgreSQL** (for relational integrity up to tens of millions of users) or **Cassandra / DynamoDB** (high-scale). Postgres is strongly recommended for its precise transactional support.
- **Partitioning**: Partition the exact schema by `conversation_id`.
  
### 3.2 Message Durability
The backend must guarantee that identical requests do not result in two messages.
Database columns: `conversation_id`, `msg_uuid` (Primary Key), `sender_id`, `sequence_id`, `text`, `created_at`.
Using `msg_uuid` as the Primary/Idempotency key natively rejects duplicates during client retries.

### 3.3 Sync API Requirements (Idempotent)
1. **`POST /api/messages` (Send Message - Batch Supported)**
   - Payload: Array of `[msg_uuid, conversation_id, text, client_timestamp]`
   - DB Transaction: Upserts row. If `ON CONFLICT (msg_uuid) DO NOTHING`, it still returns the existing `sequence_id` to the client.
2. **`GET /api/sync/messages?conversation_id=X&after_seq_id=Y&limit=100`**
   - Fetches all messages mathematically sorted logically after the client's last known `seq_id`.
3. **`POST /api/messages/read`**
   - Payload: `[conversation_id, last_read_seq_id]`
   - Shifts the water-mark for read receipts.

---

## 4. Bi-Directional Sync Mechanism

### 4.1 Client → Server Sync (Upstream)
When connectivity is restored, the `WorkManager` reads the `sync_queue`.
- Takes up to 50 pending messages.
- Posts to `POST /api/messages`.
- **Server Response**: `[{ msg_uuid: "abc", seq_id: 104, timestamp: "..." }]`.
- **Client Update**: Updates local SQLite `status = SENT`, `server_seq_id = 104`. Removes item from `sync_queue`.

### 4.2 Server → Client Sync (Downstream)
Each local conversation tracks its `highest_known_seq_id`.
1. Client requests `GET /sync/messages?after_seq_id=104`.
2. Server responds with an array of newer messages.
3. Client performs a SQLite upsert (`INSERT OR REPLACE` mapping `msg_uuid`).
4. Updates the local `highest_known_seq_id` to the maximum received.

### 4.3 Sync Triggers
- **Foregrounding the app**: Fetch the `/api/sync/conversations` snapshot.
- **Connectivity Regained**: Process `sync_queue`, then `/api/sync/messages`.
- **Push Notification Wake-up**: Use FCM/APNs "data-only" messages containing `conversation_id` and `new_seq_id`. The client wakes up in the background and hits the `GET` endpoint. (Do not put the message payload in the push notification to avoid size limits and ensure E2EE compatibility).

---

## 5. Multi-Device and Cross-User Delivery

- **Multi-Device Sync**: If User A is logged into a Phone and iPad, sending a message from the Phone pushes it to the Server. The server assigns `seq_id = 105`. The Server sends a silent push to A's iPad and B's Phone. Both devices wake up, see their local `highest_seq_id` is `< 105`, and issue a sync pull.
- **Conflict Free Reconclidation**: `msg_uuid` is globally unique. Overwrites never happen. The sync payload simply fills in the missing sequence gaps.

---

## 6. Message Ordering Guarantees (Critical Problem)

Relying on client timestamps is a fatal flaw causing misordered chats (clock skew, timezone changes). Relying purely on server timestamps causes issues when two messages arrive simultaneously in a batch.

**The Solution:** Monotonic Sequence Numbers (`seq_id`).
- The backend assigns a strictly ascending `seq_id` per conversation.
- In Postgres: Use a `BEFORE INSERT` trigger or `SELECT COALESCE(MAX(seq_id), 0) + 1 FROM messages WHERE conversation_id = ? FOR UPDATE;` to lock the conversation row and grant the next integer.
- **Source of Truth for Sorting**: The client UI must sort `ORDER BY server_seq_id ASC`.
- **Handling Pending Messages**: If `server_seq_id` is NULL (because the message is `PENDING`), the UI sorts it *after* all messages with a `server_seq_id`, falling back to `ORDER BY local_timestamp ASC` only among pending messages.

---

## 7. Conflict Resolution

- **Duplicate Messages**: Since the primary key is `msg_uuid`, `INSERT ON CONFLICT DO NOTHING` safely handles duplicate network requests over flaky connections.
- **Concurrent Offline Sends**: If Alice and Bob both type while offline, they both have `server_seq_id = NULL` locally. When reconnecting, whichever request hits the Postgres `FOR UPDATE` lock first gets `seq_id = X`, and the other gets `X + 1`. The server dictates the final interleaving, which is replicated to both clients.
- **Local Merge Rules**: During a pull sync, if the server returns a message with `msg_uuid` that already exists locally, the client updates its local row with the new `server_seq_id`, `status=DELIVERED`, and server timestamp.

---

## 8. Failure Modes and Observability

"Failed to load chats" almost always points to sync gaps or poisoned cursors.

### 8.1 Safeguards
- **Cache Corruption**: If the local SQLite schema migrations fail or data is missing, include a "Clear Cache & Resync" fallback UI hook that truncates the local tables and requests `after_seq_id=0`.
- **Sync Gaps**: If the client has `seq_id`s 1, 2, 3, and suddenly receives 7 from a push notification payload... the client **must not** construct the message directly from the push payload. It must use the push to trigger `GET sync?after_seq_id=3`, successfully pulling 4, 5, 6, and 7 to prevent holes.
- **Dead-Letter Queue**: If a message in `sync_queue` fails 10 times (HTTP 400 Bad Request), mark it as `FAILED` (red exclamation mark in UI) and remove it from the automated queue to unblock subsequent messages. Allow the user to "Tap to Retry".
- **Exponential Backoff**: If the server returns HTTP 500 or 429, the `WorkManager` must back off (e.g., 2s, 4s, 8s, 16s) to prevent a DDoS during a backend outage.

---

## 9. Performance and Scalability

- **Pagination Strategy (Keyset Pagination)**: Never use `OFFSET/LIMIT`. Always use `WHERE seq_id > ? ORDER BY seq_id ASC LIMIT 100`. This uses an index and retains O(1) performance even for conversations with millions of messages.
- **Index Design**:
  - `messages` table: `CREATE UNIQUE INDEX idx_msg_uuid ON messages(msg_uuid);`
  - `messages` table: `CREATE INDEX idx_conv_seq ON messages(conversation_id, sequence_id);`
- **Polling vs Push**: Use long-polling or WebSockets when the app is actively foregrounded. Use silent Push Notifications when the app is backgrounded to trigger an HTTP pull.

---

## 10. Summary Sync Lifecycle (Step-by-Step)

1. **User opens app**: SQLite loads messages `ORDER BY server_seq_id ASC NULLS LAST, local_timestamp ASC`.
2. **User sends "Hello"**: 
   - `msg_uuid = a1b2`
   - SQLite `messages` inserted (pending).
   - SQLite `sync_queue` inserted.
   - UI instantly renders "Hello" (faded gray/clock icon).
3. **Background Worker**:
   - Reads `sync_queue`.
   - `POST /api/messages [ { a1b2, "Hello", ... } ]`.
4. **Backend Server**:
   - `INSERT INTO messages`... assigns `seq_id = 95`. Returning `200 OK`.
   - Sends silent FCM to Bob's device: `{ conv_id: 123, new_seq: 95 }`.
5. **Client Response Handle**:
   - Worker receives `200 OK`.
   - Updates local SQLite `a1b2` with `seq_id = 95`, `status = SENT`.
   - Deletes from `sync_queue`.
   - UI icon updates to a single checkmark (✓).
6. **Bob's Device (Offline → Online)**:
   - Receives FCM. Wakes up in background.
   - `GET /api/sync?conv_id=123&after_seq_id=94`.
   - Receives `[ { a1b2, "Hello", seq=95 } ]`.
   - Writes to local SQLite. Fires OS Notification.
7. **Read Receipts**:
   - Bob opens chat. `highest_read = 95`.
   - Bob performs `POST /api/messages/read { seq: 95 }`.
   - Server routes `read` event to Alice. Alice's UI updates to double blue checkmarks (✓✓).
