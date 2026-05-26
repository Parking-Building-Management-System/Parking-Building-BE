# RFID Card Seed

## Where Seed Is Implemented

RFID card pool seeding is implemented in:

```text
src/main/resources/db/migration/V20260526111000__seed_tenant_rfid_card_pool.sql
```

This is a data-only Flyway migration. It does not change business logic, enums, entities, or API contracts.

## Card Count Rule

For every non-deleted tenant:

```text
expected minimum RFID cards = max(ceil(total non-deleted slots * 1.2), 50)
```

Cards belong to the tenant only. They are not tied to a parking, floor, zone, or slot.

Default status is:

```text
ACTIVE
```

The migration does not add `IN_USE`. Current check-in availability is determined by card status being `ACTIVE` and no active `parking_sessions` row using that card.

## Code Format

Card code uses the first slug segment as a tenant-friendly prefix:

```text
<PREFIX>-0001
<PREFIX>-0002
```

Examples:

```text
VINCOM-0001
FPT-0001
BCONS-0001
DEMO-0001
SMARTPARK-0001
```

If a prefix cannot be derived, fallback is:

```text
CARD-0001
```

The migration is idempotent through the existing unique constraint `(tenant_id, code)` and `ON CONFLICT (tenant_id, code) DO NOTHING`.

## Example Tenant/Card Counts

Based on the current rich seed:

| Tenant | Example Total Slots | Expected Minimum Cards |
|---|---:|---:|
| Vincom Mega Mall | 354 | 425 |
| FPT Tower | 280 | 336 |
| Bcons Plaza | 184 | 221 |
| Demo Parking Tower | depends on active seeded slots | `max(ceil(slots * 1.2), 50)` |
| SmartPark SaaS | usually 0 | 50 |

Actual totals can be higher because older seed files already inserted a few RFID cards.

## How To Verify

Run:

```sql
WITH expected AS (
    SELECT
        t.id AS tenant_id,
        t.name AS tenant_name,
        COUNT(s.id)::integer AS total_slots,
        GREATEST(CEIL(COUNT(s.id)::numeric * 1.2)::integer, 50) AS expected_minimum_cards
    FROM tenants t
    LEFT JOIN slots s
        ON s.tenant_id = t.id
       AND s.is_deleted = false
    WHERE t.is_deleted = false
    GROUP BY t.id, t.name
)
SELECT
    e.tenant_name,
    e.total_slots,
    COUNT(c.id)::integer AS total_rfid_cards,
    e.expected_minimum_cards
FROM expected e
LEFT JOIN rfid_cards c
    ON c.tenant_id = e.tenant_id
GROUP BY e.tenant_name, e.total_slots, e.expected_minimum_cards
ORDER BY e.tenant_name;
```

Expected result:

```text
total_rfid_cards >= expected_minimum_cards
```
