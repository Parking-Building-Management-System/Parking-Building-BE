# Demo MinIO Floor Map Seed Troubleshooting

Demo floor maps are seeded by `DemoFacilityMapSeedRunner` after Spring emits
`ApplicationReadyEvent`. The seeder reads image assets from
`src/main/resources/demo-assets/floor-maps/` and uploads them through the
configured `StorageService`.

## Required Environment

Set these values for local MinIO-backed demo map seeding:

```bash
APP_MINIO_ENDPOINT=http://localhost:9000
APP_MINIO_ACCESS_KEY=minioadmin
APP_MINIO_SECRET_KEY=minioadmin
APP_MINIO_SIGNING_REGION=minio
APP_MINIO_BUCKET=smartpark
APP_DEMO_SEED_ENABLED=true
APP_DEMO_SEED_FLOOR_MAPS_ENABLED=true
APP_DEMO_SEED_SLOT_COORDINATES_ENABLED=true
```

Do not commit real credentials. The `minioadmin` values above are only the
default local Compose credentials.

## Startup Checklist

1. Start PostgreSQL, Redis, and MinIO from `compose.yaml`.
2. Confirm MinIO is reachable at `APP_MINIO_ENDPOINT`.
3. Start the backend with the env values above.
4. Check logs for the seed summary:
   `Facility map demo seed complete: floorMapsSeeded=..., floorMapsSkippedConfigured=...`.
5. If storage is missing, the app should still start and log:
   `Demo floor map seed skipped: storage not configured.`

## Expected Seeder Behavior

- Empty `floors.map_image_url`: upload a classpath demo image and then store an
  object key like `tenants/{tenantId}/floor-maps/demo/{floorId}-demo-parking-1.png`.
- Existing object key with present MinIO object: skip without duplicate upload.
- Existing object key with missing MinIO object: re-upload the demo image to that
  same object key.
- External `http://` or `https://` map URLs: skip as already configured.
- Upload failure: log a warning and do not write a new object key.

## Verify Database State

```sql
SELECT f.id, f.code, f.name, f.map_image_url
FROM floors f
WHERE f.is_deleted = false
ORDER BY f.created_at DESC;
```

For seeded demo maps, `map_image_url` should be a tenant-scoped object key, not
a presigned URL.

## Verify MinIO Object

Using the MinIO client:

```bash
mc alias set local http://localhost:9000 minioadmin minioadmin
mc ls local/smartpark/tenants --recursive | grep floor-maps
```

You should see objects under:

```text
tenants/{tenantId}/floor-maps/demo/
```

## Verify Presigned Download

Use the manager storage presign endpoint with a manager token and the stored
object key:

```bash
curl -s "$BASE_URL/manager/storage/download-url?objectKey=$OBJECT_KEY" \
  -H "Authorization: Bearer $MANAGER_TOKEN"
```

Then open or curl the returned `downloadUrl`. A successful response should
download the image bytes before the presigned URL expires.

## Common Failure Cases

- `APP_DEMO_SEED_ENABLED=false`: no demo seeding runs.
- `APP_DEMO_SEED_FLOOR_MAPS_ENABLED=false`: slot coordinates may seed, but floor
  map images do not upload.
- Missing MinIO env: app starts, storage seed is skipped, and no object keys are
  written.
- MinIO not ready yet: app starts, upload attempts log warnings, and empty
  floors remain empty for the next restart.
- Database has object keys but MinIO was reset: restart with storage configured;
  the seeder checks object existence and re-uploads missing keys.
- Assets missing from the packaged classpath: seeder logs the missing filename
  and skips that upload.
