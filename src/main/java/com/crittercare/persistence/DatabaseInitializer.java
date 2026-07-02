package com.crittercare.persistence;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Initializes the database schema and populates it with foundational seed data.
 * <p>
 * This component executes during the application startup phase to ensure the required
 * relational schema is present. If an empty database is detected, it automatically
 * provisions sample records. It also guarantees a consistent simulation baseline
 * by resetting volatile entity statistics upon each execution.
 * </p>
 */
public class DatabaseInitializer {

    private final DatabaseManager dbManager;

    /**
     * Constructs a new database initializer with the required database manager.
     *
     * @param dbManager the manager providing active database connections
     */
    public DatabaseInitializer(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    /**
     * Executes the initialization routine for the database schema and state.
     * <p>
     * This method is idempotent regarding schema creation. It evaluates the current
     * state of the database to determine if seed data insertion is necessary, and
     * systematically resets volatile statistics.
     * </p>
     */
    public void initialize() {
        System.out.println("[DB] Initializing schema...");
        createTables();
        System.out.println("[DB] Schema ready.");

        if (isDatabaseEmpty()) {
            System.out.println("[DB] Empty database detected – seeding sample data...");
            seedData();
            System.out.println("[DB] Seed data inserted.");
        }

        // Always reset volatile stats to full on startup so each run begins fresh
        resetSimulationStats();
    }

    /**
     * Executes the Data Definition Language (DDL) statements required to build the schema.
     */
    private void createTables() {
        Connection conn = dbManager.getConnection();
        try (Statement stmt = conn.createStatement()) {

            // 1. Enclosures (no FK deps – created first)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS enclosures (
                    id                   INT AUTO_INCREMENT PRIMARY KEY,
                    name                 VARCHAR(100)  NOT NULL,
                    habitat_type         VARCHAR(50)   NOT NULL,
                    cleanliness          DOUBLE        NOT NULL DEFAULT 100.0,
                    capacity             INT           NOT NULL DEFAULT 10,
                    last_cleaned         TIMESTAMP              DEFAULT CURRENT_TIMESTAMP,
                    maintenance_schedule VARCHAR(100)           DEFAULT 'Daily'
                )
                """);

            // 2. Animals (FK → enclosures)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS animals (
                    id                   INT AUTO_INCREMENT PRIMARY KEY,
                    name                 VARCHAR(100)  NOT NULL,
                    species              VARCHAR(100)  NOT NULL,
                    type                 VARCHAR(20)   NOT NULL,
                    age                  INT           NOT NULL,
                    health               DOUBLE        NOT NULL DEFAULT 100.0,
                    hunger               DOUBLE        NOT NULL DEFAULT 0.0,
                    hydration            DOUBLE        NOT NULL DEFAULT 100.0,
                    activity_level       DOUBLE        NOT NULL DEFAULT 80.0,
                    enclosure_id         INT,
                    assigned_zookeeper   VARCHAR(100),
                    admission_date       DATE,
                    has_fur              BOOLEAN                DEFAULT FALSE,
                    fur_color            VARCHAR(50),
                    wingspan_cm          DOUBLE,
                    can_fly              BOOLEAN                DEFAULT TRUE,
                    is_venomous          BOOLEAN                DEFAULT FALSE,
                    uvb_requirement      DOUBLE                 DEFAULT 5.0,
                    CONSTRAINT fk_animal_enclosure
                        FOREIGN KEY (enclosure_id) REFERENCES enclosures(id)
                        ON DELETE SET NULL
                )
                """);

            // 3. Maintenance logs (FK → animals, enclosures)
            stmt.execute("""
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
                )
                """);

            // 4. Zookeepers (no FK deps)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS zookeepers (
                    id   INT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(100) NOT NULL
                )
                """);

            // 5. Alerts (no FK deps)
            stmt.execute("""
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
                )
                """);

        } catch (SQLException e) {
            throw new RuntimeException("Failed to create database tables.", e);
        }
    }

    /**
     * Determines whether the database is unpopulated by verifying the presence of enclosure records.
     *
     * @return {@code true} if the database contains no records, otherwise {@code false}
     */
    private boolean isDatabaseEmpty() {
        try (Statement stmt = dbManager.getConnection().createStatement();
             ResultSet rs   = stmt.executeQuery("SELECT COUNT(*) FROM enclosures")) {
            return rs.next() && rs.getInt(1) == 0;
        } catch (SQLException e) {
            return true; // Assume empty if we can't check
        }
    }

    /**
     * Restores all volatile simulation parameters to their default starting configuration.
     * <p>
     * This ensures that subsequent application executions begin with optimal animal
     * health and pristine enclosure conditions, discarding historical alert states.
     * </p>
     */
    public void resetSimulationStats() {
        Connection conn = dbManager.getConnection();
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(
                "UPDATE animals SET health=100.0, hunger=0.0, hydration=100.0, activity_level=80.0");
            stmt.execute(
                "UPDATE enclosures SET cleanliness=100.0, last_cleaned=CURRENT_TIMESTAMP");
            stmt.execute("DELETE FROM alerts");
            System.out.println("[DB] Simulation stats reset to full.");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to reset simulation stats.", e);
        }
    }

    /**
     * Populates the underlying database tables with initial system data.
     */
    private void seedData() {
        Connection conn = dbManager.getConnection();
        try (Statement stmt = conn.createStatement()) {

            // ── Enclosures ────────────────────────────────────────────────
            stmt.execute("""
                INSERT INTO enclosures (name, habitat_type, cleanliness, capacity,
                                        last_cleaned, maintenance_schedule)
                VALUES
                  ('Big Cat Ridge',     'SAVANNAH',     92.0, 4,
                   '2025-05-20 07:00:00', 'Daily'),
                  ('Penguin Point',     'ARCTIC',       88.0, 12,
                   '2025-05-20 06:30:00', 'Daily'),
                  ('Elephant Preserve', 'SAVANNAH',     95.0, 6,
                   '2025-05-20 07:00:00', 'Daily'),
                  ('Savannah Plains',   'SAVANNAH',     90.0, 8,
                   '2025-05-21 07:00:00', 'Daily'),
                  ('Primate Forest',    'FOREST',       85.0, 10,
                   '2025-05-19 08:00:00', 'Daily'),
                  ('Reptile House',     'REPTILE_HOUSE',72.0, 15,
                   '2025-05-19 09:15:00', 'Every 2 Days'),
                  ('Aviary',            'AVIARY',       58.0, 20,
                   '2025-05-19 06:30:00', 'Daily'),
                  ('Aquatic Zone',      'AQUATIC',      90.0, 8,
                   '2025-05-20 08:00:00', 'Daily')
                """);

            // ── Animals ───────────────────────────────────────────────────
            stmt.execute("""
                INSERT INTO animals (name, species, type, age, health, hunger,
                                     hydration, activity_level, enclosure_id,
                                     assigned_zookeeper, admission_date,
                                     has_fur, fur_color,
                                     wingspan_cm, can_fly,
                                     is_venomous, uvb_requirement)
                VALUES
                  -- Mammals
                  ('Leo',    'African Lion',     'MAMMAL', 7,  89.0, 45.0, 72.0, 68.0,
                   1, 'Alex Morgan',   '2018-05-18', TRUE,  'Golden',  NULL, NULL, NULL, NULL),
                  ('Maya',   'Asian Elephant',   'MAMMAL', 15, 95.0, 20.0, 85.0, 78.0,
                   3, 'Alex Morgan',   '2010-03-12', TRUE,  'Grey',    NULL, NULL, NULL, NULL),
                  ('Kibo',   'Giraffe',          'MAMMAL', 5,  91.0, 30.0, 80.0, 75.0,
                   4, 'Jordan Lee',    '2020-07-04', TRUE,  'Tan',     NULL, NULL, NULL, NULL),
                  ('Zuri',   'Giraffe',          'MAMMAL', 3,  88.0, 55.0, 70.0, 70.0,
                   4, 'Jordan Lee',    '2022-02-15', TRUE,  'Tan',     NULL, NULL, NULL, NULL),
                  ('Bruno',  'Chimpanzee',       'MAMMAL', 9,  93.0, 25.0, 88.0, 82.0,
                   5, 'Sam Patel',     '2016-08-22', FALSE, NULL,      NULL, NULL, NULL, NULL),

                  -- Birds
                  ('Pippin', 'African Penguin',  'BIRD',   4,  78.0, 65.0, 60.0, 55.0,
                   2, 'Alex Morgan',   '2021-11-10', FALSE, NULL, 35.0, FALSE, NULL, NULL),
                  ('Rio',    'Scarlet Macaw',    'BIRD',   6,  90.0, 35.0, 82.0, 77.0,
                   7, 'Sam Patel',     '2019-04-20', FALSE, NULL, 75.0, TRUE,  NULL, NULL),
                  ('Sunny',  'Bald Eagle',       'BIRD',   8,  94.0, 28.0, 85.0, 80.0,
                   7, 'Sam Patel',     '2017-09-05', FALSE, NULL, 210.0, TRUE, NULL, NULL),

                  -- Reptiles
                  ('Rex',    'Nile Crocodile',   'REPTILE',20, 92.0, 15.0, 90.0, 40.0,
                   6, 'Jordan Lee',    '2006-01-30', NULL, NULL, NULL, NULL, TRUE,  8.0),
                  ('Iggy',   'Green Iguana',     'REPTILE',3,  85.0, 40.0, 75.0, 65.0,
                   6, 'Jordan Lee',    '2022-06-18', NULL, NULL, NULL, NULL, FALSE, 6.0),
                  ('Spike',  'Komodo Dragon',    'REPTILE',12, 88.0, 50.0, 70.0, 60.0,
                   6, 'Alex Morgan',   '2013-03-11', NULL, NULL, NULL, NULL, TRUE,  7.0)
                """);

            // ── Zookeepers ────────────────────────────────────────────────
            stmt.execute("""
                INSERT INTO zookeepers (name) VALUES
                  ('Alex Morgan'),
                  ('Jordan Lee'),
                  ('Sam Patel')
                """);

            // ── Maintenance Logs ──────────────────────────────────────────
            stmt.execute("""
                INSERT INTO maintenance_logs (animal_id, enclosure_id, timestamp,
                                              activity_type, staff_member, status, notes,
                                              follow_up_needed)
                VALUES
                  (1, 1, '2025-05-20 09:15:00', 'Health Check', 'Alex Morgan',
                   'Completed', 'Leo is alert and responsive. No abnormal behavior.', FALSE),
                  (2, 3, '2025-05-20 08:30:00', 'Feeding',      'Alex Morgan',
                   'Completed', 'Fed 5.0 kg of raw meat (beef). Ate all food within 10 minutes.', FALSE),
                  (6, 2, '2025-05-19 16:45:00', 'Enrichment',   'Sam Patel',
                   'Completed', 'Added new puzzle feeder. Pippin engaged for 30 minutes.', FALSE),
                  (NULL, 6, '2025-05-19 14:10:00', 'Habitat Cleaning', 'Jordan Lee',
                   'Completed', 'Full enclosure clean. UV lamps checked and replaced.', FALSE),
                  (3, 4, '2025-05-20 08:30:00', 'Feeding',      'Jordan Lee',
                   'Completed', 'Fed 8 kg of browse. Kibo ate well.', FALSE),
                  (1, 1, '2025-05-20 08:05:00', 'Hydration Check', 'Alex Morgan',
                   'Completed', 'Water bowl refilled. Leo drank normally.', FALSE),
                  (2, 3, '2025-05-19 10:40:00', 'Health Check', 'Dr. Sarah Lee',
                   'Needs Follow-Up', 'Minor stiffness in left hind leg observed.', TRUE),
                  (NULL, 6, '2025-05-19 11:20:00', 'Intervention',  'Dr. Sarah Lee',
                   'Needs Follow-Up', 'Adjusted UVB light intensity and ambient temperature.', TRUE)
                """);

            // ── Alerts ────────────────────────────────────────────────────
            stmt.execute("""
                INSERT INTO alerts (type, severity, source_id, source_name,
                                    message, time_raised, status, resolved)
                VALUES
                  ('LOW_HEALTH',       'CRITICAL', 'ANM-1', 'Leo (Lion)',
                   'Health score has dropped below the critical threshold. Reduced appetite and low activity detected.',
                   '2025-05-20 08:30:00', 'New', FALSE),
                  ('CRITICAL_HUNGER',  'HIGH',     'ANM-6', 'Pippin (Penguin)',
                   'Hunger level exceeds 60%. Animal not eating well since May 18.',
                   '2025-05-20 08:05:00', 'New', FALSE),
                  ('DIRTY_ENCLOSURE',  'HIGH',     'ENC-7', 'Aviary',
                   'Enclosure cleanliness is at 58%, below the 70% warning threshold. Cleaning overdue.',
                   '2025-05-20 07:45:00', 'In Progress', FALSE),
                  ('LOW_HEALTH',       'WARNING',  'ANM-4', 'Zuri (Giraffe)',
                   'Weight check due today. Health trending downward over last 3 days.',
                   '2025-05-20 07:20:00', 'New', FALSE),
                  ('DIRTY_ENCLOSURE',  'WARNING',  'ENC-6', 'Reptile House',
                   'Cleanliness at 72%. Cleaning due today.',
                   '2025-05-20 06:50:00', 'Acknowledged', FALSE)
                """);

        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert seed data.", e);
        }
    }
}
