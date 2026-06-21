-- ============================================================
-- Seed: Chain Status reference data
-- ============================================================
INSERT INTO tr_chain_status (chn_sts_id, chn_sts_name)
SELECT 1, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM tr_chain_status WHERE chn_sts_id = 1);
INSERT INTO tr_chain_status (chn_sts_id, chn_sts_name)
SELECT 2, 'SUSPENDED'
WHERE NOT EXISTS (SELECT 1 FROM tr_chain_status WHERE chn_sts_id = 2);
INSERT INTO tr_chain_status (chn_sts_id, chn_sts_name)
SELECT 3, 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM tr_chain_status WHERE chn_sts_id = 3);

-- ============================================================
-- Seed: Chain
-- ============================================================
INSERT INTO ts_chain (chn_id, chn_name, chn_description, chn_sts_id)
SELECT 1, 'ORDER_PROCESSING', 'Order processing pipeline with configurable routing', 1
WHERE NOT EXISTS (SELECT 1 FROM ts_chain WHERE chn_id = 1);

-- ============================================================
-- Seed: Steps (reusable processing units)
-- ============================================================
INSERT INTO ts_step (stp_id, stp_name, stp_description)
SELECT 1,  'validateOrder',    'Validate order data (items, quantities, customer info)'
WHERE NOT EXISTS (SELECT 1 FROM ts_step WHERE stp_id = 1);
INSERT INTO ts_step (stp_id, stp_name, stp_description)
SELECT 2,  'checkInventory',   'Check stock availability for ordered items'
WHERE NOT EXISTS (SELECT 1 FROM ts_step WHERE stp_id = 2);
INSERT INTO ts_step (stp_id, stp_name, stp_description)
SELECT 3,  'processPayment',   'Process payment through payment gateway'
WHERE NOT EXISTS (SELECT 1 FROM ts_step WHERE stp_id = 3);
INSERT INTO ts_step (stp_id, stp_name, stp_description)
SELECT 4,  'applyDiscount',    'Apply loyalty and promotion discounts'
WHERE NOT EXISTS (SELECT 1 FROM ts_step WHERE stp_id = 4);
INSERT INTO ts_step (stp_id, stp_name, stp_description)
SELECT 5,  'calculateTax',     'Calculate applicable sales taxes'
WHERE NOT EXISTS (SELECT 1 FROM ts_step WHERE stp_id = 5);
INSERT INTO ts_step (stp_id, stp_name, stp_description)
SELECT 6,  'fulfillOrder',     'Pack items and arrange shipment'
WHERE NOT EXISTS (SELECT 1 FROM ts_step WHERE stp_id = 6);
INSERT INTO ts_step (stp_id, stp_name, stp_description)
SELECT 7,  'sendConfirmation', 'Send order confirmation notification'
WHERE NOT EXISTS (SELECT 1 FROM ts_step WHERE stp_id = 7);
INSERT INTO ts_step (stp_id, stp_name, stp_description)
SELECT 8,  'updateAccounting', 'Update financial ledgers and inventory records'
WHERE NOT EXISTS (SELECT 1 FROM ts_step WHERE stp_id = 8);
INSERT INTO ts_step (stp_id, stp_name, stp_description)
SELECT 9,  'escalateOrder',    'Flag order for manual review'
WHERE NOT EXISTS (SELECT 1 FROM ts_step WHERE stp_id = 9);
INSERT INTO ts_step (stp_id, stp_name, stp_description)
SELECT 10, 'archiveOrder',     'Archive completed order records'
WHERE NOT EXISTS (SELECT 1 FROM ts_step WHERE stp_id = 10);

-- ============================================================
-- Seed: Configuration 1 — Standard Order
-- ============================================================
INSERT INTO ts_chain_config (chn_cfg_id, chn_cfg_name, chn_cfg_description, chn_sts_id, chn_id)
SELECT 1, 'standard-order', 'Standard order processing pipeline', 1, 1
WHERE NOT EXISTS (SELECT 1 FROM ts_chain_config WHERE chn_cfg_id = 1);

INSERT INTO ts_chain_step (chn_stp_id, chn_stp_next_step_on_success, chn_stp_next_step_on_failure, chn_sts_id, chn_cfg_id, stp_id)
SELECT 1,  'checkInventory',   'escalateOrder',  1, 1, 1 WHERE NOT EXISTS (SELECT 1 FROM ts_chain_step WHERE chn_stp_id = 1);
INSERT INTO ts_chain_step (chn_stp_id, chn_stp_next_step_on_success, chn_stp_next_step_on_failure, chn_sts_id, chn_cfg_id, stp_id)
SELECT 2,  'processPayment',   'escalateOrder',  1, 1, 2 WHERE NOT EXISTS (SELECT 1 FROM ts_chain_step WHERE chn_stp_id = 2);
INSERT INTO ts_chain_step (chn_stp_id, chn_stp_next_step_on_success, chn_stp_next_step_on_failure, chn_sts_id, chn_cfg_id, stp_id)
SELECT 3,  'calculateTax',     'escalateOrder',  1, 1, 3 WHERE NOT EXISTS (SELECT 1 FROM ts_chain_step WHERE chn_stp_id = 3);
INSERT INTO ts_chain_step (chn_stp_id, chn_stp_next_step_on_success, chn_stp_next_step_on_failure, chn_sts_id, chn_cfg_id, stp_id)
SELECT 4,  'fulfillOrder',     'escalateOrder',  1, 1, 5 WHERE NOT EXISTS (SELECT 1 FROM ts_chain_step WHERE chn_stp_id = 4);
INSERT INTO ts_chain_step (chn_stp_id, chn_stp_next_step_on_success, chn_stp_next_step_on_failure, chn_sts_id, chn_cfg_id, stp_id)
SELECT 5,  'sendConfirmation', 'escalateOrder',  1, 1, 6 WHERE NOT EXISTS (SELECT 1 FROM ts_chain_step WHERE chn_stp_id = 5);
INSERT INTO ts_chain_step (chn_stp_id, chn_stp_next_step_on_success, chn_stp_next_step_on_failure, chn_sts_id, chn_cfg_id, stp_id)
SELECT 6,  'updateAccounting', 'escalateOrder',  1, 1, 7 WHERE NOT EXISTS (SELECT 1 FROM ts_chain_step WHERE chn_stp_id = 6);
INSERT INTO ts_chain_step (chn_stp_id, chn_stp_next_step_on_success, chn_stp_next_step_on_failure, chn_sts_id, chn_cfg_id, stp_id)
SELECT 7,  'archiveOrder',     'escalateOrder',  1, 1, 8 WHERE NOT EXISTS (SELECT 1 FROM ts_chain_step WHERE chn_stp_id = 7);
INSERT INTO ts_chain_step (chn_stp_id, chn_stp_next_step_on_success, chn_stp_next_step_on_failure, chn_sts_id, chn_cfg_id, stp_id)
SELECT 8,  NULL,               NULL,             1, 1, 10 WHERE NOT EXISTS (SELECT 1 FROM ts_chain_step WHERE chn_stp_id = 8);

-- ============================================================
-- Seed: Configuration 2 — Premium Order (with discount step)
-- ============================================================
INSERT INTO ts_chain_config (chn_cfg_id, chn_cfg_name, chn_cfg_description, chn_sts_id, chn_id)
SELECT 2, 'premium-order', 'Premium order processing with discount application', 1, 1
WHERE NOT EXISTS (SELECT 1 FROM ts_chain_config WHERE chn_cfg_id = 2);

INSERT INTO ts_chain_step (chn_stp_id, chn_stp_next_step_on_success, chn_stp_next_step_on_failure, chn_sts_id, chn_cfg_id, stp_id)
SELECT 9,  'checkInventory',   'escalateOrder',  1, 2, 1  WHERE NOT EXISTS (SELECT 1 FROM ts_chain_step WHERE chn_stp_id = 9);
INSERT INTO ts_chain_step (chn_stp_id, chn_stp_next_step_on_success, chn_stp_next_step_on_failure, chn_sts_id, chn_cfg_id, stp_id)
SELECT 10, 'processPayment',   'escalateOrder',  1, 2, 2  WHERE NOT EXISTS (SELECT 1 FROM ts_chain_step WHERE chn_stp_id = 10);
INSERT INTO ts_chain_step (chn_stp_id, chn_stp_next_step_on_success, chn_stp_next_step_on_failure, chn_sts_id, chn_cfg_id, stp_id)
SELECT 11, 'applyDiscount',    'escalateOrder',  1, 2, 3  WHERE NOT EXISTS (SELECT 1 FROM ts_chain_step WHERE chn_stp_id = 11);
INSERT INTO ts_chain_step (chn_stp_id, chn_stp_next_step_on_success, chn_stp_next_step_on_failure, chn_sts_id, chn_cfg_id, stp_id)
SELECT 12, 'calculateTax',     'escalateOrder',  1, 2, 4  WHERE NOT EXISTS (SELECT 1 FROM ts_chain_step WHERE chn_stp_id = 12);
INSERT INTO ts_chain_step (chn_stp_id, chn_stp_next_step_on_success, chn_stp_next_step_on_failure, chn_sts_id, chn_cfg_id, stp_id)
SELECT 13, 'fulfillOrder',     'escalateOrder',  1, 2, 5  WHERE NOT EXISTS (SELECT 1 FROM ts_chain_step WHERE chn_stp_id = 13);
INSERT INTO ts_chain_step (chn_stp_id, chn_stp_next_step_on_success, chn_stp_next_step_on_failure, chn_sts_id, chn_cfg_id, stp_id)
SELECT 14, 'sendConfirmation', 'escalateOrder',  1, 2, 6  WHERE NOT EXISTS (SELECT 1 FROM ts_chain_step WHERE chn_stp_id = 14);
INSERT INTO ts_chain_step (chn_stp_id, chn_stp_next_step_on_success, chn_stp_next_step_on_failure, chn_sts_id, chn_cfg_id, stp_id)
SELECT 15, 'updateAccounting', 'escalateOrder',  1, 2, 7  WHERE NOT EXISTS (SELECT 1 FROM ts_chain_step WHERE chn_stp_id = 15);
INSERT INTO ts_chain_step (chn_stp_id, chn_stp_next_step_on_success, chn_stp_next_step_on_failure, chn_sts_id, chn_cfg_id, stp_id)
SELECT 16, 'archiveOrder',     'escalateOrder',  1, 2, 8  WHERE NOT EXISTS (SELECT 1 FROM ts_chain_step WHERE chn_stp_id = 16);
INSERT INTO ts_chain_step (chn_stp_id, chn_stp_next_step_on_success, chn_stp_next_step_on_failure, chn_sts_id, chn_cfg_id, stp_id)
SELECT 17, NULL,               NULL,             1, 2, 10 WHERE NOT EXISTS (SELECT 1 FROM ts_chain_step WHERE chn_stp_id = 17);

-- ============================================================
-- Seed: Configuration 3 — Flagged Order (escalation path)
-- ============================================================
INSERT INTO ts_chain_config (chn_cfg_id, chn_cfg_name, chn_cfg_description, chn_sts_id, chn_id)
SELECT 3, 'flagged-order', 'Order flagged for manual review', 1, 1
WHERE NOT EXISTS (SELECT 1 FROM ts_chain_config WHERE chn_cfg_id = 3);

INSERT INTO ts_chain_step (chn_stp_id, chn_stp_next_step_on_success, chn_stp_next_step_on_failure, chn_sts_id, chn_cfg_id, stp_id)
SELECT 18, 'checkInventory',   'escalateOrder',  1, 3, 1  WHERE NOT EXISTS (SELECT 1 FROM ts_chain_step WHERE chn_stp_id = 18);
INSERT INTO ts_chain_step (chn_stp_id, chn_stp_next_step_on_success, chn_stp_next_step_on_failure, chn_sts_id, chn_cfg_id, stp_id)
SELECT 19, 'escalateOrder',    'escalateOrder',  1, 3, 2  WHERE NOT EXISTS (SELECT 1 FROM ts_chain_step WHERE chn_stp_id = 19);
INSERT INTO ts_chain_step (chn_stp_id, chn_stp_next_step_on_success, chn_stp_next_step_on_failure, chn_sts_id, chn_cfg_id, stp_id)
SELECT 20, 'archiveOrder',     'archiveOrder',   1, 3, 9  WHERE NOT EXISTS (SELECT 1 FROM ts_chain_step WHERE chn_stp_id = 20);
INSERT INTO ts_chain_step (chn_stp_id, chn_stp_next_step_on_success, chn_stp_next_step_on_failure, chn_sts_id, chn_cfg_id, stp_id)
SELECT 21, NULL,               NULL,             1, 3, 10 WHERE NOT EXISTS (SELECT 1 FROM ts_chain_step WHERE chn_stp_id = 21);
