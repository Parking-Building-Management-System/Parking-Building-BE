# Demo MinIO Floor Map Seed Troubleshooting

Demo floor maps are seeded by `DemoFacilityMapSeedRunner` after Spring emits
`ApplicationReadyEvent`. Flyway creates the demo tenants, parkings, floors, and
slots first; the Spring seeder then reads bundled images from
`src/main/resources/demo-assets/floor-maps/` and uploads them through the
configured `StorageService`.

The seed is disabled by default. MinIO running by itself is not enough; the
backend process must receive both `APP_DEMO_SEED_*` and `APP_MINIO_*` values.

## Required local env

Host-run backend:

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

Docker-run backend:

```bash
APP_MINIO_ENDPOINT=http://minio:9000
APP_MINIO_ACCESS_KEY=minioadmin
APP_MINIO_SECRET_KEY=minioadmin
APP_MINIO_SIGNING_REGION=minio
APP_MINIO_BUCKET=smartpark
APP_DEMO_SEED_ENABLED=true
APP_DEMO_SEED_FLOOR_MAPS_ENABLED=true
APP_DEMO_SEED_SLOT_COORDINATES_ENABLED=true
```

Use `http://minio:9000` only when the backend container is on the same Docker
Compose network as the `minio` service. With the current root `compose.yaml`,
only dependencies are containerized, so a backend started from the host shell
uses `http://localhost:9000`.

Do not commit real credentials. The `minioadmin` values above are local Compose
defaults.

## Startup order

1. `docker compose up -d minio`
2. Verify MinIO health:

   ```bash
   curl -fsS http://localhost:9000/minio/health/live
   ```

3. Start the backend with the required env.
4. Check logs for:

   ```text
   Facility map demo seed summary: demoSeedEnabled=..., floorMapsEnabled=...
   ```

5. Check DB.
6. Check bucket objects.
7. Run the presign download test.

## Environment checks

Host-run backend:

```bash
printenv | grep '^APP_' | sort
```

Docker-run backend:

```bash
docker compose exec <backend-service> printenv | grep '^APP_' | sort
docker compose exec <backend-service> curl -fsS http://minio:9000/minio/health/live
```

The current repo `compose.yaml` has no backend service. Replace
`<backend-service>` only if your local setup adds one.

## DB checks

```sql
select count(*) as total_floors,
count(map_image_url) as floors_with_map
from floors
where is_deleted = false;

select id, name, map_image_url
from floors
where is_deleted = false
order by name;

select count(*) from tenants;

select count(*) from floors where is_deleted = false;
```

For seeded demo maps, `map_image_url` should be a tenant-scoped object key like:

```text
tenants/{tenantId}/floor-maps/demo/{floorId}-demo-parking-1.png
```

It should not be a presigned URL.

## MinIO checks

Console:

```text
http://localhost:9001
```

MinIO client:

```bash
mc alias set local http://localhost:9000 minioadmin minioadmin
mc ls local/smartpark
mc ls local/smartpark/tenants --recursive | grep floor-maps
```

Expected object prefix:

```text
tenants/{tenantId}/floor-maps/demo/
```

If the DB has a `map_image_url`, list that exact tenant path:

```bash
mc ls local/smartpark/tenants/<tenantId>/floor-maps/ --recursive
```

## API checks

Get the stored object key:

```bash
curl -s "$BASE_URL/manager/floors/$FLOOR_ID/map" \
  -H "Authorization: Bearer $MANAGER_TOKEN"
```

Presign the download:

```bash
curl -s "$BASE_URL/manager/storage/presign-download?objectKey=$OBJECT_KEY" \
  -H "Authorization: Bearer $MANAGER_TOKEN"
```

Then curl the returned `downloadUrl`:

```bash
curl -I "$DOWNLOAD_URL"
```

Expected result is HTTP 200 before the URL expires.

## Expected seeder behavior

- Empty `floors.map_image_url`: upload a classpath demo image, then store the
  object key in DB.
- Existing object key with present MinIO object: skip without duplicate upload.
- Existing object key with missing MinIO object: re-upload the demo image to the
  same object key.
- External `http://` or `https://` map URLs: skip as already configured.
- Missing MinIO env: app starts, floor-map upload is skipped, and the startup
  summary reports storage as unconfigured.
- Upload failure: log a warning and do not write a new object key.

## Common fixes

- Env flags missing: set all three `APP_DEMO_SEED_*` flags to `true`.
- Wrong endpoint inside Docker: use `http://minio:9000` for backend containers,
  but `http://localhost:9000` for a host-run backend.
- Stale DB with objectKey but missing object: restart the current backend with
  storage configured; the seeder checks object existence and re-uploads missing
  demo keys.
- MinIO bucket missing: verify `APP_MINIO_BUCKET=smartpark`; the storage service
  creates the bucket when credentials allow it.
- App started before MinIO: start MinIO first, verify health, then restart the
  backend.
- Old backend commit before the idempotent reupload fix: pull the current commit
  and restart with the seed flags enabled.
- Browser CORS issue vs backend seed issue: first prove the backend can presign
  and the presigned URL returns HTTP 200. CORS affects browser access, not
  whether the backend seeded objects.
