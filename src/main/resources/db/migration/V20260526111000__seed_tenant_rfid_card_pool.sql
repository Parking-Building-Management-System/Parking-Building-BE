WITH tenant_card_plan AS (
    SELECT
        t.id AS tenant_id,
        UPPER(
            COALESCE(
                NULLIF(regexp_replace(split_part(t.slug, '-', 1), '[^a-zA-Z0-9]', '', 'g'), ''),
                'CARD'
            )
        ) AS card_prefix,
        COUNT(s.id)::integer AS total_slots,
        GREATEST(CEIL(COUNT(s.id)::numeric * 1.2)::integer, 50) AS expected_card_count
    FROM tenants t
    LEFT JOIN slots s
        ON s.tenant_id = t.id
       AND s.is_deleted = false
    WHERE t.is_deleted = false
    GROUP BY t.id, t.slug
),
generated_cards AS (
    SELECT
        tenant_id,
        card_prefix,
        total_slots,
        expected_card_count,
        generated.card_no
    FROM tenant_card_plan
    CROSS JOIN LATERAL generate_series(1, expected_card_count) AS generated(card_no)
)
INSERT INTO rfid_cards (id, tenant_id, code, uid, assigned_user_id, status, activated_at, expired_at)
SELECT
    md5('rfid-card-pool:' || tenant_id::text || ':' || card_no::text)::uuid,
    tenant_id,
    card_prefix || '-' || LPAD(card_no::text, 4, '0'),
    'UID-' || tenant_id::text || '-' || LPAD(card_no::text, 4, '0'),
    NULL,
    'ACTIVE',
    CURRENT_TIMESTAMP,
    NULL
FROM generated_cards
ON CONFLICT (tenant_id, code) DO NOTHING;
