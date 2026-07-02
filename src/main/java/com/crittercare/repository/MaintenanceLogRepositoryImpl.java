package com.crittercare.repository;

import com.crittercare.model.MaintenanceLog;
import com.crittercare.persistence.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Provides a JDBC-based implementation of the {@link MaintenanceLogRepository} interface.
 * <p>
 * This repository manages the persistence of maintenance and care logs. It handles
 * the flexible relational mapping where a log entry may be optionally associated
 * with an animal, an enclosure, or both, ensuring referential integrity through
 * nullable foreign keys.
 * </p>
 */
public class MaintenanceLogRepositoryImpl implements MaintenanceLogRepository {

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


    private final DatabaseManager dbManager;

    /**
     * Constructs a new repository implementation.
     *
     * @param dbManager the database manager providing JDBC connections
     */
    public MaintenanceLogRepositoryImpl(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    /** {@inheritDoc} */
    @Override
    public List<MaintenanceLog> findAll() {
        return query(FIND_ALL);
    }

    /** {@inheritDoc} */
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

    /** {@inheritDoc} */
    @Override
    public List<MaintenanceLog> findByAnimalId(int animalId) {
        return queryWithInt(FIND_BY_ANIMAL, animalId);
    }

    /** {@inheritDoc} */
    @Override
    public List<MaintenanceLog> findByEnclosureId(int enclosureId) {
        return queryWithInt(FIND_BY_ENCLOSURE, enclosureId);
    }

    /** {@inheritDoc} */
    @Override
    public List<MaintenanceLog> findRecent(int limit) {
        return queryWithInt(FIND_RECENT, limit);
    }

    /** {@inheritDoc} */
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

    /** {@inheritDoc} */
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

    /** {@inheritDoc} */
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

    /**
     * Maps the current row of the provided {@link ResultSet} to a new {@link MaintenanceLog} entity.
     *
     * @param rs the active result set
     * @return a fully populated maintenance log instance
     * @throws SQLException if a column label is invalid or a database error occurs
     */
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

    /**
     * Executes a parameterless SQL SELECT statement and maps the results to a list of entities.
     *
     * @param sql the SQL statement to execute
     * @return a list of mapped maintenance logs
     */
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

    /**
     * Executes an SQL SELECT statement with a single integer parameter and maps the results.
     *
     * @param sql   the parameterized SQL statement
     * @param param the integer value to bind to the statement
     * @return a list of mapped maintenance logs
     */
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
     * Binds the state of a {@link MaintenanceLog} entity to the provided {@link PreparedStatement}.
     * <p>
     * Ensures that unassigned relational identifiers are mapped to SQL NULL values.
     * </p>
     *
     * @param stmt the statement to be populated
     * @param log  the entity containing the required state
     * @throws SQLException if setting a parameter fails
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
