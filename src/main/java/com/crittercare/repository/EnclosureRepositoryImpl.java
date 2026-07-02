package com.crittercare.repository;

import com.crittercare.model.Enclosure;
import com.crittercare.model.HabitatType;
import com.crittercare.persistence.DatabaseManager;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of EnclosureRepository.
 *
 * The enclosures table does not store animal IDs directly — animals hold
 * a reference to their enclosure via the enclosure_id foreign key.
 * loadAnimalIds() runs a second lightweight query to populate the
 * Enclosure.animalIds list after the main row is mapped.
 */
public class EnclosureRepositoryImpl implements EnclosureRepository {

    // ── SQL constants ────────────────────────────────────────────────────────

    private static final String FIND_ALL = """
            SELECT id, name, habitat_type, cleanliness, capacity,
                   last_cleaned, maintenance_schedule
            FROM enclosures
            ORDER BY id
            """;

    private static final String FIND_BY_ID = """
            SELECT id, name, habitat_type, cleanliness, capacity,
                   last_cleaned, maintenance_schedule
            FROM enclosures
            WHERE id = ?
            """;

    private static final String FIND_BY_HABITAT = """
            SELECT id, name, habitat_type, cleanliness, capacity,
                   last_cleaned, maintenance_schedule
            FROM enclosures
            WHERE habitat_type = ?
            ORDER BY id
            """;

    private static final String LOAD_ANIMAL_IDS =
            "SELECT id FROM animals WHERE enclosure_id = ? ORDER BY id";

    private static final String INSERT = """
            INSERT INTO enclosures
              (name, habitat_type, cleanliness, capacity, last_cleaned, maintenance_schedule)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

    private static final String UPDATE = """
            UPDATE enclosures SET
              name = ?, habitat_type = ?, cleanliness = ?, capacity = ?,
              last_cleaned = ?, maintenance_schedule = ?
            WHERE id = ?
            """;

    private static final String DELETE = "DELETE FROM enclosures WHERE id = ?";

    // ── Dependencies ─────────────────────────────────────────────────────────

    private final DatabaseManager dbManager;

    public EnclosureRepositoryImpl(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    // ── Interface implementation ─────────────────────────────────────────────

    @Override
    public List<Enclosure> findAll() {
        List<Enclosure> enclosures = new ArrayList<>();
        try (Statement stmt = dbManager.getConnection().createStatement();
             ResultSet rs   = stmt.executeQuery(FIND_ALL)) {
            while (rs.next()) {
                Enclosure enc = mapRow(rs);
                loadAnimalIds(enc);
                enclosures.add(enc);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch all enclosures.", e);
        }
        return enclosures;
    }

    @Override
    public Optional<Enclosure> findById(int id) {
        try (PreparedStatement stmt =
                     dbManager.getConnection().prepareStatement(FIND_BY_ID)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Enclosure enc = mapRow(rs);
                    loadAnimalIds(enc);
                    return Optional.of(enc);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch enclosure id=" + id, e);
        }
        return Optional.empty();
    }

    @Override
    public List<Enclosure> findByHabitatType(HabitatType type) {
        List<Enclosure> enclosures = new ArrayList<>();
        try (PreparedStatement stmt =
                     dbManager.getConnection().prepareStatement(FIND_BY_HABITAT)) {
            stmt.setString(1, type.name());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Enclosure enc = mapRow(rs);
                    loadAnimalIds(enc);
                    enclosures.add(enc);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to fetch enclosures of type " + type, e);
        }
        return enclosures;
    }

    @Override
    public Enclosure save(Enclosure enclosure) {
        try (PreparedStatement stmt =
                     dbManager.getConnection().prepareStatement(
                             INSERT, Statement.RETURN_GENERATED_KEYS)) {

            setParams(stmt, enclosure);
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    enclosure.setId(keys.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to save enclosure: " + enclosure.getName(), e);
        }
        return enclosure;
    }

    @Override
    public void update(Enclosure enclosure) {
        try (PreparedStatement stmt =
                     dbManager.getConnection().prepareStatement(UPDATE)) {

            setParams(stmt, enclosure);            // params 1–6
            stmt.setInt(7, enclosure.getId());     // WHERE id = ?
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to update enclosure id=" + enclosure.getId(), e);
        }
    }

    @Override
    public void delete(int id) {
        try (PreparedStatement stmt =
                     dbManager.getConnection().prepareStatement(DELETE)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete enclosure id=" + id, e);
        }
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private Enclosure mapRow(ResultSet rs) throws SQLException {
        Enclosure enc = new Enclosure();
        enc.setId(rs.getInt("id"));
        enc.setName(rs.getString("name"));
        enc.setHabitatType(HabitatType.valueOf(rs.getString("habitat_type")));
        enc.setCleanliness(rs.getDouble("cleanliness"));
        enc.setCapacity(rs.getInt("capacity"));

        Timestamp lastCleaned = rs.getTimestamp("last_cleaned");
        if (lastCleaned != null) {
            enc.setLastCleaned(lastCleaned.toLocalDateTime());
        }

        enc.setMaintenanceSchedule(rs.getString("maintenance_schedule"));
        return enc;
    }

    /**
     * Queries the animals table to find which animal IDs belong to this
     * enclosure, then populates enclosure.animalIds.
     * Runs one extra query per enclosure — acceptable for small datasets.
     */
    private void loadAnimalIds(Enclosure enclosure) {
        List<Integer> ids = new ArrayList<>();
        try (PreparedStatement stmt =
                     dbManager.getConnection().prepareStatement(LOAD_ANIMAL_IDS)) {
            stmt.setInt(1, enclosure.getId());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getInt("id"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to load animal ids for enclosure id="
                    + enclosure.getId(), e);
        }
        enclosure.setAnimalIds(ids);
    }

    /** Binds the 6 parameters shared by INSERT (1–6) and UPDATE (1–6). */
    private void setParams(PreparedStatement stmt, Enclosure enc)
            throws SQLException {

        stmt.setString(1, enc.getName());
        stmt.setString(2, enc.getHabitatType().name());
        stmt.setDouble(3, enc.getCleanliness());
        stmt.setInt   (4, enc.getCapacity());

        LocalDateTime lc = enc.getLastCleaned();
        stmt.setTimestamp(5, lc != null
                ? Timestamp.valueOf(lc)
                : Timestamp.valueOf(LocalDateTime.now()));

        stmt.setString(6, enc.getMaintenanceSchedule());
    }
}
