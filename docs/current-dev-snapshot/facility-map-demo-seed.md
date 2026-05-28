# Facility Map Demo Seed

## Purpose

This seed is for local/dev/demo testing only. It helps exercise Manager Facility Map APIs and the public PWA card guide without requiring every developer to manually upload floor maps and place slot pins.

The seed is disabled by default.

## Demo Assets

Bundled map images live under:

```text
src/main/resources/demo-assets/floor-maps/
```

Current assets:

- `demo-parking-1.png`
- `demo-parking-2.png`
- `demo-parking-3.png`
- `demo-parking-4.jpg`
- `demo-parking-5.jpg`

Images are bundled instead of downloaded from the internet so local seed runs are deterministic, offline-friendly, and do not depend on third-party availability or licensing changes.

Flyway does not upload files to MinIO. Flyway remains responsible for database schema/data migrations only; object storage writes are performed by the Spring Boot demo seeder after the app starts and storage config is available.

## Configuration

YAML keys:

```yaml
app:
  demo-seed:
    enabled: false
    floor-maps-enabled: false
    slot-coordinates-enabled: false
```

Environment variables:

```bash
APP_DEMO_SEED_ENABLED=true
APP_DEMO_SEED_FLOOR_MAPS_ENABLED=true
APP_DEMO_SEED_SLOT_COORDINATES_ENABLED=true
```

Floor-map upload also requires MinIO/S3 config:

```bash
APP_MINIO_ENDPOINT=http://localhost:9000
APP_MINIO_ACCESS_KEY=minioadmin
APP_MINIO_SECRET_KEY=minioadmin
APP_MINIO_SIGNING_REGION=minio
APP_MINIO_BUCKET=smartpark
```

If demo seed is disabled, the seeder does not run. If floor-map seeding is enabled but MinIO/S3 is not configured, image upload is skipped with a clear warning and coordinate seeding can still run.

## Object Keys

For each non-deleted floor whose `map_image_url` is null or blank, the seeder picks one bundled asset deterministically from the floor ID and uploads it to:

```text
tenants/{tenantId}/floor-maps/demo/{floorId}-{assetFileName}
```

The stored `floors.map_image_url` value is that object key. Existing `map_image_url` values are preserved and never overwritten.

## Coordinates

For each non-deleted slot where either `x_coordinate` or `y_coordinate` is missing, the seeder generates percent coordinates. Slots with both coordinates already set are preserved.

Algorithm:

- Group slots by floor, then zone.
- Sort zones by name, code, then ID.
- Sort slots naturally by code, so `C-02` comes before `C-10`.
- Assign vertical bands by zone index:
  - zone 0: `18..30`
  - zone 1: `38..52`
  - zone 2: `62..78`
  - zone 3+: `82..92`
- Within each zone:
  - `columns = min(12, max(4, ceil(sqrt(slotCount * 1.6))))`
  - `rows = ceil(slotCount / columns)`
  - `x = 12 + col * (76 / max(columns - 1, 1))`
  - `y = bandStart + row * ((bandEnd - bandStart) / max(rows - 1, 1))`
- Values are clamped to `5..95` and rounded to 2 decimals.

Generated coordinates are approximate demo pins. Managers can adjust slot pins manually through the normal Facility Map APIs.

## Idempotency

- Existing floor maps are skipped.
- Existing slots with both coordinates are skipped.
- Slots with one missing coordinate receive a deterministic regenerated `x/y` pair.
- Demo object keys are deterministic. Re-uploading to the same demo object path is acceptable for local/demo data.
- Manager-configured floor maps and complete manual coordinates are not overwritten.

## Run Locally

```bash
APP_MINIO_ENDPOINT=http://localhost:9000 \
APP_MINIO_ACCESS_KEY=minioadmin \
APP_MINIO_SECRET_KEY=minioadmin \
APP_MINIO_SIGNING_REGION=minio \
APP_MINIO_BUCKET=smartpark \
APP_DEMO_SEED_ENABLED=true \
APP_DEMO_SEED_FLOOR_MAPS_ENABLED=true \
APP_DEMO_SEED_SLOT_COORDINATES_ENABLED=true \
./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

At startup, logs include:

- floor maps seeded count
- floor maps skipped because already configured
- slot coordinates seeded count
- slot coordinates skipped because already configured
- whether floor-map upload was skipped because storage was unavailable

## Validate

Floor maps:

```sql
select t.slug, p.name as parking, f.name as floor, f.map_image_url
from floors f
join parkings p on p.id = f.parking_id
join tenants t on t.id = p.tenant_id
where f.is_deleted = false
order by t.slug, p.name, f.name;
```

Coordinate coverage:

```sql
select
  t.slug,
  p.name as parking,
  f.name as floor,
  count(*) as total_slots,
  count(*) filter (where s.x_coordinate is not null and s.y_coordinate is not null) as mapped_slots
from slots s
join zones z on z.id = s.zone_id
join floors f on f.id = z.floor_id
join parkings p on p.id = f.parking_id
join tenants t on t.id = p.tenant_id
where s.is_deleted = false
group by t.slug, p.name, f.name
order by t.slug, p.name, f.name;
```

Bad coordinates:

```sql
select code, x_coordinate, y_coordinate
from slots
where x_coordinate < 0 or x_coordinate > 100
   or y_coordinate < 0 or y_coordinate > 100;
```

API checks:

- `GET /manager/floors/{floorId}/map` returns `mapImageUrl` and mapped slots.
- After staff check-in, `GET /pwa/cards/{qrToken}/active-session` returns `mapDisplayUrl`, `xCoordinate`, and `yCoordinate` for the assigned slot when an active session exists.

## Known Limitations

- Generated coordinates are approximate and may not perfectly match the image.
- Managers can adjust pins manually.
- Floor-map image upload requires MinIO/S3 config.
- This seed is not intended for production data.
