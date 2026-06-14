-- =============================================================
--  CritterCare – H2 Database Schema
--  Order matters: enclosures has no FK deps, animals depends
--  on enclosures, logs depend on both.
-- =============================================================

-- ── 1. Enclosures ────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS enclosures (
    id                   INT AUTO_INCREMENT PRIMARY KEY,
    name                 VARCHAR(100)  NOT NULL,
    habitat_type         VARCHAR(50)   NOT NULL,
    cleanliness          DOUBLE        NOT NULL DEFAULT 100.0,
    capacity             INT           NOT NULL DEFAULT 10,
    last_cleaned         TIMESTAMP              DEFAULT CURRENT_TIMESTAMP,
    maintenance_schedule VARCHAR(100)           DEFAULT 'Daily'
);

-- ── 2. Animals ───────────────────────────────────────────────
--  Single-table inheritance: one row per animal regardless of
--  subtype. Subtype-specific columns are nullable.
CREATE TABLE IF NOT EXISTS animals (
    id                   INT AUTO_INCREMENT PRIMARY KEY,
    name                 VARCHAR(100)  NOT NULL,
    species              VARCHAR(100)  NOT NULL,
    type                 VARCHAR(20)   NOT NULL, -- MAMMAL | BIRD | REPTILE
    age                  INT           NOT NULL,
    health               DOUBLE        NOT NULL DEFAULT 100.0,
    hunger               DOUBLE        NOT NULL DEFAULT 0.0,
    hydration            DOUBLE        NOT NULL DEFAULT 100.0,
    activity_level       DOUBLE        NOT NULL DEFAULT 80.0,
    enclosure_id         INT,
    assigned_zookeeper   VARCHAR(100),
    admission_date       DATE,
    -- Mammal-specific columns
    has_fur              BOOLEAN                DEFAULT FALSE,
    fur_color            VARCHAR(50),
    -- Bird-specific columns
    wingspan_cm          DOUBLE,
    can_fly              BOOLEAN                DEFAULT TRUE,
    -- Reptile-specific columns
    is_venomous          BOOLEAN                DEFAULT FALSE,
    uvb_requirement      DOUBLE                 DEFAULT 5.0,

    CONSTRAINT fk_animal_enclosure
        FOREIGN KEY (enclosure_id) REFERENCES enclosures(id)
        ON DELETE SET NULL
);

-- ── 3. Maintenance Logs ──────────────────────────────────────
CREATE TABLE IF NOT EXISTS maintenance_logs (
    id                   INT AUTO_INCREMENT PRIMARY KEY,
    animal_id            INT,
    enclosure_id         INT,
    timestamp            TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    activity_type        VARCHAR(100)  NOT NULL,
    staff_member         VARCHAR(100)  NOT NULL,
    status               VARCHAR(50)            DEFAULT 'Completed',
    notes                VARCHAR(1000),
    follow_up_needed     BOOLEAN                DEFAULT FALSE,

    CONSTRAINT fk_log_animal
        FOREIGN KEY (animal_id) REFERENCES animals(id)
        ON DELETE SET NULL,
    CONSTRAINT fk_log_enclosure
        FOREIGN KEY (enclosure_id) REFERENCES enclosures(id)
        ON DELETE SET NULL
);

-- ── 4. Alerts ────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS alerts (
    id                   INT AUTO_INCREMENT PRIMARY KEY,
    type                 VARCHAR(50)   NOT NULL,
    severity             VARCHAR(20)   NOT NULL,
    source_id            VARCHAR(50),
    source_name          VARCHAR(100),
    message              VARCHAR(1000),
    time_raised          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status               VARCHAR(50)            DEFAULT 'New',
    resolved             BOOLEAN                DEFAULT FALSE
);
