-- ============================================================
-- Chain Status reference table
-- ============================================================
CREATE TABLE IF NOT EXISTS tr_chain_status (
    chn_sts_id INTEGER PRIMARY KEY,
    chn_sts_name VARCHAR(50) NOT NULL,
    chn_sts_creation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    chn_sts_update_date TIMESTAMP
);

-- ============================================================
-- Chain: logical workflow definition
-- ============================================================
CREATE TABLE IF NOT EXISTS ts_chain (
    chn_id INTEGER AUTO_INCREMENT PRIMARY KEY,
    chn_name VARCHAR(100) NOT NULL,
    chn_description VARCHAR(500),
    chn_sts_id INTEGER DEFAULT 1,
    chn_creation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    chn_update_date TIMESTAMP
);

-- ============================================================
-- Step: reusable processing unit
-- ============================================================
CREATE TABLE IF NOT EXISTS ts_step (
    stp_id INTEGER AUTO_INCREMENT PRIMARY KEY,
    stp_name VARCHAR(100) NOT NULL,
    stp_description VARCHAR(500),
    stp_creation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    stp_update_date TIMESTAMP
);

-- ============================================================
-- Chain Configuration: named variant of a chain
-- ============================================================
CREATE TABLE IF NOT EXISTS ts_chain_config (
    chn_cfg_id INTEGER AUTO_INCREMENT PRIMARY KEY,
    chn_cfg_name VARCHAR(100) NOT NULL,
    chn_cfg_description VARCHAR(500),
    chn_sts_id INTEGER DEFAULT 1,
    chn_id INTEGER NOT NULL,
    chn_cfg_creation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    chn_cfg_update_date TIMESTAMP,
    FOREIGN KEY (chn_id) REFERENCES ts_chain(chn_id)
);

-- ============================================================
-- Chain Step: step binding with routing rules
-- ============================================================
CREATE TABLE IF NOT EXISTS ts_chain_step (
    chn_stp_id INTEGER AUTO_INCREMENT PRIMARY KEY,
    chn_stp_next_step_on_success VARCHAR(100),
    chn_stp_next_step_on_failure VARCHAR(100),
    chn_sts_id INTEGER DEFAULT 1,
    chn_cfg_id INTEGER NOT NULL,
    stp_id INTEGER NOT NULL,
    chn_stp_creation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    chn_stp_update_date TIMESTAMP,
    FOREIGN KEY (chn_cfg_id) REFERENCES ts_chain_config(chn_cfg_id),
    FOREIGN KEY (stp_id) REFERENCES ts_step(stp_id)
);

-- Index for the decider query (by step name + config name)
CREATE INDEX IF NOT EXISTS ix_chain_step_lookup
    ON ts_chain_step (chn_cfg_id, stp_id);
