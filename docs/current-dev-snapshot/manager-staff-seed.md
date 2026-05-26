# Manager Staff Seed

## Discovered Seed Chain

Flyway migration order:

1. `V20260515023500__init_tenant_table.sql`
2. `V20260516162100__init_identity_tables.sql`
3. `V20260518093000__seed_identity_auth.sql`
4. `V20260520100000__init_core_domain_tables.sql`
5. `V20260520110000__seed_core_data.sql`
6. `V20260520110500__seed_core_facility_data.sql`
7. `V20260520130000__add_manager_facility_metadata.sql`
8. `V20260520130500__backfill_rich_facility_metadata.sql`
9. `V20260526111000__seed_tenant_rfid_card_pool.sql`
10. `V20260526142000__seed_demo_staff_accounts.sql`

Actual table names used by this project:

- `tenants`
- `users`
- `roles`
- `user_roles`
- `devices`
- `sessions`
- `parkings`
- `floors`
- `zones`
- `slots`
- `rfid_cards`

## Current Seed Inventory

Counts below are for non-deleted facility rows as defined by the migrations.

| Tenant | Tenant ID/Slug | Parkings | Floors | Zones | Slots | RFID Cards | Existing Managers | Existing Staff |
|---|---|---:|---:|---:|---:|---:|---:|---:|
| SmartPark SaaS | `md5('smartpark-saas')`, `smartpark-saas` | 0 | 0 | 0 | 0 | 50 | 0 | 0 |
| Demo Parking Tower | `md5('demo-parking-tower')`, `demo-parking-tower` | 0 | 0 | 0 | 0 | 50 | 1 | 1 |
| Vincom Mega Mall | `md5('tenant-vincom')`, `vincom-mega-mall` | 2 | 6 | 18 | 354 | 426 | 1 | 4 |
| FPT Tower | `md5('tenant-fpt')`, `fpt-tower` | 1 | 3 | 7 | 280 | 337 | 1 | 4 |
| Bcons Plaza | `md5('tenant-bcons')`, `bcons-plaza` | 1 | 2 | 6 | 184 | 222 | 1 | 4 |

`Existing Staff` includes the original single seeded staff account plus the three extra demo staff
accounts added by this migration for each facility tenant.

## Migration Added

File:

```text
src/main/resources/db/migration/V20260526142000__seed_demo_staff_accounts.sql
```

The migration creates three additional `STAFF` users for each seeded tenant that actually has
non-deleted parking, floor, zone, and slot data:

- `staff01@<tenant-slug>.smartpark.local`
- `staff02@<tenant-slug>.smartpark.local`
- `cashier01@<tenant-slug>.smartpark.local`

Seeded facility tenants:

- `vincom-mega-mall`
- `fpt-tower`
- `bcons-plaza`

Example accounts:

- `staff01@bcons-plaza.smartpark.local`
- `staff02@bcons-plaza.smartpark.local`
- `cashier01@bcons-plaza.smartpark.local`

Default local/dev password:

```text
Password@123
```

Password hash convention reused from existing seed migrations:

```text
$2a$10$q4lJo1nNnql5H3n5g5b14unZ2/B4I.EeIOfJM/E/raMU7okw1E5Fm
```

## Device Policy Notes

- This migration inserts only `users` and `user_roles`.
- It does not insert `devices`.
- It does not insert `sessions`.
- It does not bind staff to kiosks.
- Newly seeded staff devices are intentionally not trusted yet.
- Staff first login from a new device should still fail with the existing strict device policy and
  create or keep a pending device request.

The older seed migrations already include some original demo staff devices. Those are legacy seed
rows and are not modified here.

## SQL Verification

Required staff check:

```sql
select t.slug, u.username, u.full_name, u.status, r.name as role_name
from users u
join tenants t on t.id = u.tenant_id
join user_roles ur on ur.user_id = u.id
join roles r on r.id = ur.role_id
where r.name = 'STAFF'
order by t.slug, u.username;
```

Confirm new demo staff have no devices:

```sql
select t.slug, u.username, d.id as device_id, d.status as device_status
from users u
join tenants t on t.id = u.tenant_id
left join devices d on d.user_id = u.id
where u.username like 'staff01@%.smartpark.local'
   or u.username like 'staff02@%.smartpark.local'
   or u.username like 'cashier01@%.smartpark.local'
order by t.slug, u.username;
```

Confirm no sessions were seeded:

```sql
select t.slug, u.username, s.id as session_id, s.revoked_at, s.expired_at
from users u
join tenants t on t.id = u.tenant_id
left join sessions s on s.user_id = u.id
where u.username like 'staff01@%.smartpark.local'
   or u.username like 'staff02@%.smartpark.local'
   or u.username like 'cashier01@%.smartpark.local'
order by t.slug, u.username;
```

Facility tenant count check:

```sql
select
    t.slug,
    count(distinct p.id) filter (where p.is_deleted = false) as parkings,
    count(distinct f.id) filter (where f.is_deleted = false) as floors,
    count(distinct z.id) filter (where z.is_deleted = false) as zones,
    count(distinct s.id) filter (where s.is_deleted = false) as slots,
    count(distinct rc.id) as rfid_cards
from tenants t
left join parkings p on p.tenant_id = t.id
left join floors f on f.tenant_id = t.id
left join zones z on z.tenant_id = t.id
left join slots s on s.tenant_id = t.id
left join rfid_cards rc on rc.tenant_id = t.id
where t.is_deleted = false
group by t.slug
order by t.slug;
```

Check idempotency:

```sql
select username, count(*)
from users
where username like '%@%.smartpark.local'
group by username
having count(*) > 1;
```
