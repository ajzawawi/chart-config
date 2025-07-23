

| Component           | Bits   | Example        | Purpose                                                                  |
| ------------------- | ------ | -------------- | ------------------------------------------------------------------------ |
| **Timestamp**       | 48-bit | `017FC21C5E7A` | Encodes **physical time** (in ms since epoch). ULID-style timestamp.     |
| **Logical Counter** | 16-bit | `0001`         | Ensures strict ordering for multiple events in the **same millisecond**. |
| **Publisher ID**    | 16-bit | `000A`         | Unique ID per AMPS publisher (so IDs don’t clash).                       |
| **Randomness**      | 48-bit | `A1B2C3D4E5F6` | Tie-breaker for concurrency or multi-threaded publishing.                |

Publisher A

Trade#1 → 017FC21C5E7A-0000-000A-A1B2C3D4E5F6
Trade#2 → 017FC21C5E7A-0001-000A-B3C4D5E6F708
Trade#3 → 017FC21C5E7A-0002-000A-1C2D3E4F5061

Publisher B

Trade#1 → 017FC21C5E79-0000-000B-C9D8E7F6A5B4
Trade#2 → 017FC21C5E79-0001-000B-02B4C8D7E6F0
Trade#3 → 017FC21C5E79-0002-000B-1234ABCD5678


Sort
017FC21C5E79-0000-000B-C9D8E7F6A5B4   (B: Trade#1)
017FC21C5E79-0001-000B-02B4C8D7E6F0   (B: Trade#2)
017FC21C5E79-0002-000B-1234ABCD5678   (B: Trade#3)
017FC21C5E7A-0000-000A-A1B2C3D4E5F6   (A: Trade#1)
017FC21C5E7A-0001-000A-B3C4D5E6F708   (A: Trade#2)
017FC21C5E7A-0002-000A-1C2D3E4F5061   (A: Trade#3)
