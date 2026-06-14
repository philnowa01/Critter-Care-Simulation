package com.crittercare.repository;

import com.crittercare.model.MaintenanceLog;
import com.crittercare.persistence.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of MaintenanceLogRepository.
 *
 * Both animal_id and enclosure_id are nullable FKs in the database —
 * a log may reference only an enclosure (sanitation), only an animal
 * (health check), or both.  rs.wasNull() is used after every getInt()
 * call on these columns; Java 0 is stored back only when the DB value
 * is genuinely zero (which it never is for an auto-increment PK).
 */
public class MaintenanceLogRepositoryImpl implements MaintenanceLogRepository {

    // ── SQL constants ────────────────────────────────────────────────────────

    private static final String SELECT_COLS = """
            SELECT id, animal_id, enclosure_id, timestamp,
                   activity_type, staff_member, status, notes, follow_up_needed
            FROM maintenance_logs
            """;

    private static final String FIND_ALL =
            SELECT_COLS + "ORDER BY timestamp DESC";

    private static final String FIND_BY_ID =
            SELECT_COLS + "WHERE id = ?";

    private static final String FIND_BY_ANIMAL =
            SELECT_COLS + "WHERE animal_id = ? ORDER BY timestamp DESC";

    private static final String FIND_BY_ENCLOSURE =
            SELECT_COLS + "WHERE enclosure_id = ? ORDER BY timestamp DESC";

    private static final String FIND_RECENT =
            SELECT_COLS + "ORDER BY timestamp DESC LIMIT ?";

    private static final String INSERT = """
            INSERT INTO maintenance_logs
              (animal_id, enclosure_id, timestamp, activity_type,
               staff_member, status, notes, follow_up_needed)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String UPDATE = """
            UPDATE maintenance_logs SET
              animal_id = ?, enclosure_id = ?, timestamp = ?,
              activity_type = ?, staff_member = ?, status = ?,
              notes = ?, follow_up_needed = ?
            WHERE id = ?
            """;

    private static final String DELETE =
            "DELETE FROM maintenance_logs WHERE id = ?";

    // ── Dependencies ─────────────────────────────────────────────────────────

    private final DatabaseManager dbManager;

    public MaintenanceLogRepositoryImpl(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    // ── Interface implementation ─────────────────────────────────────────────

    @Override
    public List<MaintenanceLog> findAll() {
        return query(FIND_ALL);
    }

    @Override
    public Optional<MaintenanceLog> findById(int id) {
        try (PreparedStatement stmt =
                     dbManager.getConnection().prepareStatement(FIND_BY_ID)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch log id=" + id, e);
        }
        return Optional.empty();
    }

    @Override
    public List<MaintenanceLog> findByAnimalId(int animalId) {
        return queryWithInt(FIND_BY_ANIMAL, animalId);
    }

    @Override
    public List<MaintenanceLog> findByEnclosureId(int enclosureId) {
        return queryWithInt(FIND_BY_ENCLOSURE, enclosureId);
    }

    @Override
    public List<MaintenanceLog> findRecent(int limit) {
        return queryWithInt(FIND_RECENT, limit);
    }

    @Override
    public MaintenanceLog save(MaintenanceLog log) {
        try (PreparedStatement stmt =
                     dbManager.getConnection().prepareStatement(
                             INSERT, Statement.RETURN_GENERATED_KEYS)) {

            setParams(stmt, log);
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    log.setId(keys.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save maintenance log.", e);
        }
        return log;
    }

    @Override
    public void update(MaintenanceLog log) {
        try (PreparedStatement stmt =
                     dbManager.getConnection().prepareStatement(UPDATE)) {
            setParams(stmt, log);          // params 1–8
            stmt.setInt(9, log.getId());   // WHERE id = ?
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to update maintenance log id=" + log.getId(), e);
        }
    }

    @Override
    public void delete(int id) {
        try (PreparedStatement stmt =
                     dbManager.getConnection().prepareStatement(DELETE)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete log id=" + id, e);
        }
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private MaintenanceLog mapRow(ResultSet rs) throws SQLException {
        MaintenanceLog log = new MaintenanceLog();
        log.setId(rs.getInt("id"));

        int animalId = rs.getInt("animal_id");
        if (!rs.wasNull()) {
            log.setAnimalId(animalId);
        }

        int enclosureId = rs.getInt("enclosure_id");
        if (!rs.wasNull()) {
            log.setEnclosureId(enclosureId);
        }

        Timestamp ts = rs.getTimestamp("timestamp");
        if (ts != null) {
            log.setTimestamp(ts.toLocalDateTime());
        }

        log.setActivityType(rs.getString("activity_type"));
        log.setStaffMember(rs.getString("staff_member"));
        log.setStatus(rs.getString("status"));
        log.setNotes(rs.getString("notes"));
        log.setFollowUpNeeded(rs.getBoolean("follow_up_needed"));

        return log;
    }

    /** Runs a no-parameter SELECT and collects all rows. */
    private List<MaintenanceLog> query(String sql) {
        List<MaintenanceLog> result = new ArrayList<>();
        try (Statement stmt = dbManager.getConnection().createStatement();
             ResultSet rs   = stmt.executeQuery(sql)) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to execute query: " + sql, e);
        }
        return result;
    }

    /** Runs a single-integer-parameter SELECT and collects all rows. */
    private List<MaintenanceLog> queryWithInt(String sql, int param) {
        List<MaintenanceLog> result = new ArrayList<>();
        try (PreparedStatement stmt =
                     dbManager.getConnection().prepareStatement(sql)) {
            stmt.setInt(1, param);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to execute query with param=" + param, e);
        }
        return result;
    }

    /**
     * Binds the 8 data parameters shared by INSERT and UPDATE.
     * Nullable FKs are explicitly set to SQL NULL when the Java value is 0.
     */
    private void setParams(PreparedStatement stmt, MaintenanceLog log)
            throws SQLException {

        if (log.getAnimalId() > 0) {
            stmt.setInt(1, log.getAnimalId());
        } else {
            stmt.setNull(1, Types.INTEGER);
        }

        if (log.getEnclosureId() > 0) {
            stmt.setInt(2, log.getEnclosureId());
        } else {
            stmt.setNull(2, Types.INTEGER);
        }

        stmt.setTimestamp(3, log.getTimestamp() != null
                ? Timestamp.valueOf(log.getTimestamp())
                : new Timestamp(System.currentTimeMillis()));

        stmt.setString (4, log.getActivityType());
        stmt.setString (5, log.getStaffMember());
        stmt.setString (6, log.getStatus());
        stmt.setString (7, log.getNotes());
        stmt.setBoolean(8, log.isFollowUpNeeded());
    }
}
