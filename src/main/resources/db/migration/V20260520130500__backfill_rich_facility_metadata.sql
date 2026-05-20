INSERT INTO floors (id, tenant_id, parking_id, code, name, display_order, is_active, is_deleted)
SELECT DISTINCT
    md5('floor:' || z.tenant_id::text || ':' || z.parking_id::text || ':' || z.floor_name)::uuid,
    z.tenant_id,
    z.parking_id,
    z.floor_name,
    CASE
        WHEN z.floor_name = 'G' THEN 'Ground'
        WHEN z.floor_name = 'L1' THEN 'Level 1'
        WHEN z.floor_name = 'L2' THEN 'Level 2'
        ELSE 'Basement ' || substring(z.floor_name from 2)
    END,
    CASE
        WHEN z.floor_name = 'G' THEN 0
        WHEN z.floor_name = 'L1' THEN 1
        WHEN z.floor_name = 'L2' THEN 2
        WHEN z.floor_name = 'B1' THEN 1
        WHEN z.floor_name = 'B2' THEN 2
        WHEN z.floor_name = 'B3' THEN 3
        ELSE 99
    END,
    true,
    false
FROM zones z
WHERE z.floor_name IS NOT NULL
  AND z.floor_name <> ''
  AND z.is_deleted = false
ON CONFLICT (tenant_id, parking_id, code) DO UPDATE
SET
    name = EXCLUDED.name,
    display_order = EXCLUDED.display_order,
    is_active = EXCLUDED.is_active,
    is_deleted = EXCLUDED.is_deleted;

UPDATE zones z
SET
    floor_id = f.id,
    vehicle_type_id = CASE
        WHEN z.code LIKE '%-B' OR z.code LIKE '%BIKE%' THEN md5('vehicle-type-motorbike')::uuid
        WHEN z.code LIKE '%-E' OR z.code LIKE '%EV%' THEN md5('vehicle-type-electric-car')::uuid
        ELSE md5('vehicle-type-car')::uuid
    END
FROM floors f
WHERE f.tenant_id = z.tenant_id
  AND f.parking_id = z.parking_id
  AND f.code = z.floor_name
  AND z.is_deleted = false;

UPDATE slots s
SET floor_id = z.floor_id
FROM zones z
WHERE z.id = s.zone_id
  AND s.is_deleted = false
  AND z.floor_id IS NOT NULL;
