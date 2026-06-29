package com.crittercare.repository;

import com.crittercare.model.Alert;
import com.crittercare.model.AlertSeverity;
import com.crittercare.model.AlertType;
import com.crittercare.persistence.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of AlertRepository.
 */
public class AlertRepositoryImpl implements AlertRepository {

    // ── SQL constants ────────────────────────────────────────────────────────

    private static final String SELECT_COLS = """
            SELECT id, type, severity, source_id, source_name,
                   message, time_raised, status, resolved
            FROM alerts
            """;

    private static final String FIND_ALL    = SELECT_COLS + "ORDER BY time_raised DESC";
    private static final String FIND_ACTIVE = SELECT_COLS + "WHERE resolved = FALSE ORDER BY time_raised DESC";
    private static final String FIND_BY_ID  = SELECT_COLS + "WHERE id = ?";

    private static final String INSERT = """
            INSERT INTO alerts
              (type, severity, source_id, source_name, message,
               time_raised, status, resolved)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String UPDATE = """
            UPDATE alerts SET
              type = ?, severity = ?, source_id = ?, source_name = ?,
              message = ?, time_raised = ?, status = ?, resolved = ?
            WHERE id = ?
            """;

    private static final String DELETE = "DELETE FROM alerts WHERE id = ?";

    // ── Dependencies ─────────────────────────────────────────────────────────

    private final DatabaseManager dbManager;

    public AlertRepositoryImpl(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    // ── Interface implementation ─────────────────────────────────────────────

    @Override
    public List<Alert> findAll() {
        List<Alert> alerts = new ArrayList<>();
        try (Statement stmt = dbManager.getConnection().createStatement();
             ResultSet rs   = stmt.executeQuery(FIND_ALL)) {
            while (rs.next()) {
                alerts.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch all alerts.", e);
        }
        return alerts;
    }

    @Override
    public List<Alert> findActive() {
        List<Alert> alerts = new ArrayList<>();
        try (Statement stmt = dbManager.getConnection().createStatement();
             ResultSet rs   = stmt.executeQuery(FIND_ACTIVE)) {
            while (rs.next()) {
                alerts.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch active alerts.", e);
        }
        return alerts;
    }

    @Override
    public Optional<Alert> findById(int id) {
        try (PreparedStatement stmt =
                     dbManager.getConnection().prepareStatement(FIND_BY_ID)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch alert id=" + id, e);
        }
        return Optional.empty();
    }

    @Override
    public Alert save(Alert alert) {
        try (PreparedStatement stmt =
                     dbManager.getConnection().prepareStatement(
                             INSERT, Statement.RETURN_GENERATED_KEYS)) {
            setParams(stmt, alert);
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    alert.setId(keys.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save alert.", e);
        }
        return alert;
    }

    @Override
    public void update(Alert alert) {
        try (PreparedStatement stmt =
                     dbManager.getConnection().prepareStatement(UPDATE)) {
            setParams(stmt, alert);           // params 1–8
            stmt.setInt(9, alert.getId());    // WHERE id = ?
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to update alert id=" + alert.getId(), e);
        }
    }

    @Override
    public void delete(int id) {
        try (PreparedStatement stmt =
                     dbManager.getConnection().prepareStatement(DELETE)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete alert id=" + id, e);
        }
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private Alert mapRow(ResultSet rs) throws SQLException {
        Alert alert = new Alert();
        alert.setId(rs.getInt("id"));
        alert.setType(AlertType.valueOf(rs.getString("type")));
        alert.setSeverity(AlertSeverity.valueOf(rs.getString("severity")));
        alert.setSourceId(rs.getString("source_id"));
        alert.setSourceName(rs.getString("source_name"));
        alert.setMessage(rs.getString("message"));

        Timestamp ts = rs.getTimestamp("time_raised");
        if (ts != null) {
            alert.setTimeRaised(ts.toLocalDateTime());
        }

        alert.setStatus(rs.getString("status"));
        alert.setResolved(rs.getBoolean("resolved"));
        return alert;
    }

    private void setParams(PreparedStatement stmt, Alert alert)
            throws SQLException {
        stmt.setString   (1, alert.getType().name());
        stmt.setString   (2, alert.getSeverity().name());
        stmt.setString   (3, alert.getSourceId());
        stmt.setString   (4, alert.getSourceName());
        stmt.setString   (5, alert.getMessage());
        stmt.setTimestamp(6, alert.getTimeRaised() != null
                ? Timestamp.valueOf(alert.getTimeRaised())
                : new Timestamp(System.currentTimeMillis()));
        stmt.setString   (7, alert.getStatus());
        stmt.setBoolean  (8, alert.isResolved());
    }
}
