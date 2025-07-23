# ULID

ULIDs are a good start:

[ 48-bit Timestamp ][ 80-bit Randomness ]

But ULIDs only are not good for scenarios where you need logical ordering, can encounter clock skew issues, or have sharding/publisher contexts, and need total ordering across publishers.


# HLC-ULID
[ 48-bit Timestamp ][ 16-bit Logical Counter ][ 16-bit Publisher ID ][ 48-bit Randomness/Tie-breaker ]

Break it down to:

48-bit Timestamp:
    Same as a ULID timestamp
    Milliseconds since Unix Epoch
    Ensures lexicographic sorting

16-bit Logical Counter:
    The Hybrid logical clock (HLC) component
    Incremented if:
        Multiple events are produced in the same millisecond, or
        The system clock goes backward (to ensure monotonic IDs)

[ 16-bit Publisher ID ]
    Unique id per publisher
    Not typed to amps
    Ensures that multiple publishers can generate IDs without collissions
    Can be derived from DC + Hash(hostname)


[ 48-bit Randomness/Tie-breaker ]
    Section can include:
        Random values to ensure randomness when multiple threads or processes generate events in parallel
        Or other metadata ( like a shard ID for example)

        Not strictly needed for single threaded producers but necessary in distributed environments

# How to Genrate
    1. Get the current timestamp (milliseconds since Epoch)
    2. Compare with last timestamp
        If current > last, reset logical counter to 0
        If current == last, increment the logical counter
        If current < last, use last timestamp and increment logical counter (to correct clock skew)

    3. Append the unique publisher id (e.g: DC + Hash(hostname))
    4. Add random 48-bit value
    5. Format into a ULID-style string 

# Why This Should Work
    Ordering: Sorting by this ID is equivalent to sorting by (timestamp, logical counter, publisherId, randomness)
    Time Queries: The first 48 bits map to a real timestamp enabling range queries
    Uniqueness: No two publishers or events in the same ms produce the same ID

# Stepping Through an Example
| Publisher | Event | Timestamp (UTC)         | Notes                               |
| --------- | ----- | ----------------------- | ----------------------------------- |
| A         | 1     | 2025-07-22 10:00:00.100 | A’s first event.                    |
| A         | 2     | 2025-07-22 10:00:00.101 | A’s second event (overlap).         |
| A         | 3     | 2025-07-22 10:00:00.101 | A’s third event (same ms as A2).    |
| B         | 1     | 2025-07-22 10:00:00.101 | B’s first event (same ms as A2/A3). |
| B         | 2     | 2025-07-22 10:00:00.102 | B’s second event.                   |
| A         | 4     | 2025-07-22 10:00:00.103 | A’s fourth event.                   |

Assume Events are Now Out of Order (network issues, clock skew, insert favorite unfortunate event)

| Publisher | Event | Timestamp (UTC)         | Notes                        |
| --------- | ----- | ----------------------- | ---------------------------- |
| A         | 3     | 2025-07-22 10:00:00.101 | Arrived early but not first. |
| B         | 1     | 2025-07-22 10:00:00.101 | Arrived after A3.            |
| A         | 1     | 2025-07-22 10:00:00.100 | Arrived late, though older.  |
| A         | 2     | 2025-07-22 10:00:00.101 | Arrived after A1.            |
| B         | 2     | 2025-07-22 10:00:00.102 | Arrived next.                |
| A         | 4     | 2025-07-22 10:00:00.103 | Arrived last.                |

Try sorting by timestamp (it should be broken)

| Publisher | Event | Timestamp (UTC)         | Why Ambiguous?        | Notes                           |
| --------- | ----- | ----------------------- | --------------------- | ------------------------------- |
| A         | 1     | 2025-07-22 10:00:00.100 | Unique ms.            | Correct placement.              |
| A         | 3     | 2025-07-22 10:00:00.101 | Same ms as A2 and B1. | Order among A2, A3, B1 unknown. |
| A         | 2     | 2025-07-22 10:00:00.101 | Same ms as A3 and B1. | Order among A2, A3, B1 unknown. |
| B         | 1     | 2025-07-22 10:00:00.101 | Same ms as A2 and A3. | Order among A2, A3, B1 unknown. |
| B         | 2     | 2025-07-22 10:00:00.102 | Unique ms.            | Correct placement.              |
| A         | 4     | 2025-07-22 10:00:00.103 | Unique ms.            | Correct placement.              |


| Component           | Bits   | Example        | Purpose                                                                  |
| ------------------- | ------ | -------------- | ------------------------------------------------------------------------ |
| **Timestamp**       | 48-bit | `017FC21C5E7A` | Encodes **physical time** (in ms since epoch). ULID-style timestamp.     |
| **Logical Counter** | 16-bit | `0001`         | Ensures strict ordering for multiple events in the **same millisecond**. |
| **Publisher ID**    | 16-bit | `000A`         | Unique ID per ublisher (so IDs don’t clash).                       |
| **Randomness**      | 48-bit | `A1B2C3D4E5F6` | Tie-breaker for concurrency or multi-threaded publishing.                |


## Sorted by Event Time, Not Arrival with an HLC-ULID

| Publisher | Event | Timestamp (UTC)         | HLC-ULID Example                      | Notes                         |
| --------- | ----- | ----------------------- | ------------------------------------- | ----------------------------- |
| A         | 1     | 2025-07-22 10:00:00.100 | `017FC21C5E7A-0000-000A-AAA111111111` | A1, unique timestamp.         |
| A         | 2     | 2025-07-22 10:00:00.101 | `017FC21C5E7B-0000-000A-AAA222222222` | A2, first event in overlap.   |
| A         | 3     | 2025-07-22 10:00:00.101 | `017FC21C5E7B-0001-000A-AAA333333333` | A3, second event (counter=1). |
| B         | 1     | 2025-07-22 10:00:00.101 | `017FC21C5E7B-0000-000B-BBB444444444` | B1, first event in overlap.   |
| B         | 2     | 2025-07-22 10:00:00.102 | `017FC21C5E7C-0000-000B-BBB555555555` | B2, unique timestamp.         |
| A         | 4     | 2025-07-22 10:00:00.103 | `017FC21C5E7D-0000-000A-AAA666666666` | A4, unique timestamp.         |


## Out of Order Arrival (Annotated with HLC-ULID)

Imagine this is what the queue has and the datastore now has to process it

| Publisher | Event | Timestamp (UTC)         | HLC-ULID Example                      |
| --------- | ----- | ----------------------- | ------------------------------------- |
| A         | 3     | 2025-07-22 10:00:00.101 | `017FC21C5E7B-0001-000A-AAA333333333` |
| B         | 1     | 2025-07-22 10:00:00.101 | `017FC21C5E7B-0000-000B-BBB444444444` |
| A         | 1     | 2025-07-22 10:00:00.100 | `017FC21C5E7A-0000-000A-AAA111111111` |
| A         | 2     | 2025-07-22 10:00:00.101 | `017FC21C5E7B-0000-000A-AAA222222222` |
| B         | 2     | 2025-07-22 10:00:00.102 | `017FC21C5E7C-0000-000B-BBB555555555` |
| A         | 4     | 2025-07-22 10:00:00.103 | `017FC21C5E7D-0000-000A-AAA666666666` |


## Sort by HLC-ULID
Sorting lexicographically by HLC-ULID produces the correct global event order:

| Publisher | Event | Timestamp (UTC)         | HLC-ULID Example                      | Notes                               |
| --------- | ----- | ----------------------- | ------------------------------------- | ----------------------------------- |
| A         | 1     | 2025-07-22 10:00:00.100 | `017FC21C5E7A-0000-000A-AAA111111111` | Earliest event.                     |
| A         | 2     | 2025-07-22 10:00:00.101 | `017FC21C5E7B-0000-000A-AAA222222222` | A2 before B1 due to publisher ID.   |
| B         | 1     | 2025-07-22 10:00:00.101 | `017FC21C5E7B-0000-000B-BBB444444444` | B1 sorted after A2.                 |
| A         | 3     | 2025-07-22 10:00:00.101 | `017FC21C5E7B-0001-000A-AAA333333333` | A3 comes after A2 & B1 (counter=1). |
| B         | 2     | 2025-07-22 10:00:00.102 | `017FC21C5E7C-0000-000B-BBB555555555` | B2 is next in order.                |
| A         | 4     | 2025-07-22 10:00:00.103 | `017FC21C5E7D-0000-000A-AAA666666666` | A4 is last in order.                |

## Optional

You can influence the sorting behavior even further if you encode priorities/weights into the publisher id portion of the HLC-ULID. This allows you to enforce a consistent, domain-specific ordering across publishers, data centers or regions.

The above assumes publisher ids are just treated lexicographically (000A < 000B)

* You can define priority rules for data centers where higher priority DCs always sort before others with identical timestamps

* Alternatively, you can assign a higher weight for a specific region

* You can modify this simply by calculating a weighted hash prepending a priority nibble or byte based on the DC priority

## Benefits

1. Global Ordering

HLC-ULID: provides a globally comparable, monotonic ID across all publishers and AMPS instances

Bookmarks: Linked to a transaction log, not reliable with multiple AMPS instances and replicated logs

2. Time Aware IDs

HLC-ULID: Encodes real-world timestamps (first 48 bits) allowing time-based queries and range scans directly on the id

Bookmarks: Contain no timestamp information. It's an opaque string, you have no real insight on what the internals are. You require a separate timestamp field for queries and analytics
```
SELECT *
FROM mytable
WHERE hlc_ulid >= '017FC21C0000-0000-0000-000000000000'
  AND hlc_ulid <= '017FC21C7FFF-FFFF-FFFF-FFFFFFFFFFFF'
ORDER BY hlc_ulid;

```

3. Event Ordering in the Same Millisecond

HLC-ULID: Uses a logical counter to maintain strict ordering of events from the same publisher within a single millisecond, even if they share the same timestamp

Bookmarks: Ordering is determined only by AMPS log sequence, which does not preserve the true event order if multiple publishers are involved

4. Cross-Publisher Consistency

HLC-ULID: Publisher ids ensure unique and sortable IDs across multiple data centers and publishers. You can easily figure out which instance published what and when

Bookmarks: Are independent per publisher and per AMPS instance making global ordering impossible without extra metadata

5. Portability

HLC-ULID: The id is self-contained and meaningful outside of AMPS, which is crucial for storing and querying in historical databases or analytics systems

Bookmarks: AMPS specific and have no meaning outside of the AMPS context

6. Scalability
HLC-ULID: Works across multiple AMPS clusters, regions, or publishers witohut requiring coordination
Bookmarks: Cannot guarantee global uniqueness or ordering across multiple AMPS instances

7. Querying and Indexing
HLC-ULID: Can directly be sorted lexicographhy and indexed in databases for time-based and sequence based queries

Bookmarks: Cannot be used as sort keys for time-based queries, you need extra columns or lookup mappings

Sample Queries:

Events by a specific publisher 
```
SELECT *
FROM mytable
WHERE SUBSTR(id, 22, 4) = '000A'
ORDER BY id;
```

Publisher specific time range query:
```
SELECT *
FROM mytable
WHERE id BETWEEN '017FC21C0000-0000-000A-000000000000'
              AND '017FC21C7FFF-FFFF-000A-FFFFFFFFFFFF'
ORDER BY id;
```

8. Fault Tolerance & Replay
HLC-ULID: Replay or backfill events without breaking global order - IDs remains stable

Bookmarks: Tied to specific log offsets;

9. Clock Skew Handling

HLC-ULID: HLCs prevent issues from system clock drift by incrementing the logical counter when time goes backward

Bookmarks: Do not encode time or handle clock skew

