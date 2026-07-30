ALTER TABLE pricing_rules
DROP CONSTRAINT IF EXISTS ck_pricing_rules_daily_cap_price;

ALTER TABLE pricing_rules
DROP COLUMN daily_cap_price;
