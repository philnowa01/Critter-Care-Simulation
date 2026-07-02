package com.crittercare.repository;

import com.crittercare.model.Animal;
import com.crittercare.model.Bird;
import com.crittercare.model.Mammal;
import com.crittercare.model.Reptile;
import com.crittercare.persistence.DatabaseManager;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Provides a JDBC-based implementation of the {@link AnimalRepository} interface.
 * <p>
 * This repository utilizes a single-table inheritance strategy to persist all
 * {@link Animal} subtypes within a unified database structure. It handles the
 * translation between relational rows and polymorphic domain entities by utilizing
 * a type discriminator column.
 * </p>
 */
public class AnimalRepositoryImpl implements AnimalRepository {

    private static final String FIND_ALL = """
            SELECT id, name, species, type, age,
                   health, hunger, hydration, activity_level,
                   enclosure_id, assigned_zookeeper, admission_date,
                   has_fur, fur_color,
                   wingspan_cm, can_fly,
                   is_venomous, uvb_requirement
            FROM animals
            ORDER BY id
            """;

    private static final String FIND_BY_ID = FIND_ALL.replace(
            "ORDER BY id",
            "WHERE id = ?");

    private static final String FIND_BY_ENCLOSURE = """
            SELECT id, name, species, type, age,
                   health, hunger, hydration, activity_level,
                   enclosure_id, assigned_zookeeper, admission_date,
                   has_fur, fur_color,
                   wingspan_cm, can_fly,
                   is_venomous, uvb_requirement
            FROM animals
            WHERE enclosure_id = ?
            ORDER BY id
            """;

    private static final String INSERT = """
            INSERT INTO animals
              (name, species, type, age, health, hunger, hydration, activity_level,
               enclosure_id, assigned_zookeeper, admission_date,
               has_fur, fur_color, wingspan_cm, can_fly, is_venomous, uvb_requirement)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            """;

    private static final String UPDATE = """
            UPDATE animals SET
              name = ?, species = ?, type = ?, age = ?,
              health = ?, hunger = ?, hydration = ?, activity_level = ?,
              enclosure_id = ?, assigned_zookeeper = ?, admission_date = ?,
              has_fur = ?, fur_color = ?,
              wingspan_cm = ?, can_fly = ?,
              is_venomous = ?, uvb_requirement = ?
            WHERE id = ?
            """;

    private static final String DELETE = "DELETE FROM animals WHERE id = ?";

    private final DatabaseManager dbManager;

    /**
     * Constructs a new repository implementation.
     *
     * @param dbManager the database manager providing JDBC connections
     */
    public AnimalRepositoryImpl(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }


    /** {@inheritDoc} */
    @Override
    public List<Animal> findAll() {
        List<Animal> animals = new ArrayList<>();
        try (Statement stmt = dbManager.getConnection().createStatement();
             ResultSet rs   = stmt.executeQuery(FIND_ALL)) {
            while (rs.next()) {
                animals.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch all animals.", e);
        }
        return animals;
    }

    /** {@inheritDoc} */
    @Override
    public Optional<Animal> findById(int id) {
        try (PreparedStatement stmt =
                     dbManager.getConnection().prepareStatement(FIND_BY_ID)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch animal id=" + id, e);
        }
        return Optional.empty();
    }

    /** {@inheritDoc} */
    @Override
    public List<Animal> findByEnclosureId(int enclosureId) {
        List<Animal> animals = new ArrayList<>();
        try (PreparedStatement stmt =
                     dbManager.getConnection().prepareStatement(FIND_BY_ENCLOSURE)) {
            stmt.setInt(1, enclosureId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    animals.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to fetch animals for enclosure id=" + enclosureId, e);
        }
        return animals;
    }

    /** {@inheritDoc} */
    @Override
    public Animal save(Animal animal) {
        try (PreparedStatement stmt =
                     dbManager.getConnection().prepareStatement(
                             INSERT, Statement.RETURN_GENERATED_KEYS)) {

            setCommonParams(stmt, animal);   // params 1–17
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    animal.setId(keys.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save animal: " + animal.getName(), e);
        }
        return animal;
    }

    /** {@inheritDoc} */
    @Override
    public void update(Animal animal) {
        try (PreparedStatement stmt =
                     dbManager.getConnection().prepareStatement(UPDATE)) {

            setCommonParams(stmt, animal);      // params 1–17
            stmt.setInt(18, animal.getId());    // WHERE id = ?
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update animal id=" + animal.getId(), e);
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
            throw new RuntimeException("Failed to delete animal id=" + id, e);
        }
    }

    /**
     * Maps the current row of the provided {@link ResultSet} to the appropriate {@link Animal} subclass.
     *
     * @param rs the active result set
     * @return a fully populated animal entity matching its specific subtype
     * @throws SQLException if a column label is invalid or a database error occurs
     */
    private Animal mapRow(ResultSet rs) throws SQLException {

        String type = rs.getString("type");

        Animal animal = switch (type) {
            case "MAMMAL"  -> buildMammal(rs);
            case "BIRD"    -> buildBird(rs);
            case "REPTILE" -> buildReptile(rs);
            default        -> throw new IllegalStateException("Unknown animal type: " + type);
        };

        animal.setId(rs.getInt("id"));
        animal.setName(rs.getString("name"));
        animal.setSpecies(rs.getString("species"));
        animal.setAge(rs.getInt("age"));
        animal.setHealth(rs.getDouble("health"));
        animal.setHunger(rs.getDouble("hunger"));
        animal.setHydration(rs.getDouble("hydration"));
        animal.setActivityLevel(rs.getDouble("activity_level"));

        int encId = rs.getInt("enclosure_id");
        if (!rs.wasNull()) {
            animal.setEnclosureId(encId);
        }

        animal.setAssignedZookeeper(rs.getString("assigned_zookeeper"));

        Date admDate = rs.getDate("admission_date");
        if (admDate != null) {
            animal.setAdmissionDate(admDate.toLocalDate());
        }

        return animal;
    }

    /**
     * Instantiates and populates a {@link Mammal} entity from the result set.
     *
     * @param rs the active result set
     * @return the populated mammal instance
     * @throws SQLException if column reading fails
     */
    private Mammal buildMammal(ResultSet rs) throws SQLException {
        Mammal mammal = new Mammal();
        mammal.setHasFur(rs.getBoolean("has_fur"));
        mammal.setFurColor(rs.getString("fur_color"));
        return mammal;
    }

    /**
     * Instantiates and populates a {@link Bird} entity from the result set.
     *
     * @param rs the active result set
     * @return the populated bird instance
     * @throws SQLException if column reading fails
     */
    private Bird buildBird(ResultSet rs) throws SQLException {
        Bird bird = new Bird();
        double wingspan = rs.getDouble("wingspan_cm");
        if (!rs.wasNull()) {
            bird.setWingspanCm(wingspan);
        }
        bird.setCanFly(rs.getBoolean("can_fly"));
        return bird;
    }

    /**
     * Instantiates and populates a {@link Reptile} entity from the result set.
     *
     * @param rs the active result set
     * @return the populated reptile instance
     * @throws SQLException if column reading fails
     */
    private Reptile buildReptile(ResultSet rs) throws SQLException {
        Reptile reptile = new Reptile();
        reptile.setVenomous(rs.getBoolean("is_venomous"));
        double uvb = rs.getDouble("uvb_requirement");
        if (!rs.wasNull()) {
            reptile.setUvbRequirement(uvb);
        }
        return reptile;
    }

    /**
     * Binds the state of an {@link Animal} entity to the provided {@link PreparedStatement}.
     * Nullifies subtype-specific columns that do not apply to the current entity type.
     *
     * @param stmt   the statement to be populated
     * @param animal the entity containing the required state
     * @throws SQLException if setting a parameter fails
     */
    private void setCommonParams(PreparedStatement stmt, Animal animal)
            throws SQLException {

        stmt.setString(1, animal.getName());
        stmt.setString(2, animal.getSpecies());
        stmt.setString(3, animal.getType());
        stmt.setInt   (4, animal.getAge());
        stmt.setDouble(5, animal.getHealth());
        stmt.setDouble(6, animal.getHunger());
        stmt.setDouble(7, animal.getHydration());
        stmt.setDouble(8, animal.getActivityLevel());

        if (animal.getEnclosureId() > 0) {
            stmt.setInt(9, animal.getEnclosureId());
        } else {
            stmt.setNull(9, Types.INTEGER);
        }

        stmt.setString(10, animal.getAssignedZookeeper());

        LocalDate ad = animal.getAdmissionDate();
        stmt.setDate(11, ad != null ? Date.valueOf(ad) : null);

        // Subtype-specific columns
        if (animal instanceof Mammal m) {
            stmt.setBoolean(12, m.isHasFur());
            stmt.setString (13, m.getFurColor());
            stmt.setNull   (14, Types.DOUBLE);
            stmt.setNull   (15, Types.BOOLEAN);
            stmt.setNull   (16, Types.BOOLEAN);
            stmt.setNull   (17, Types.DOUBLE);
        } else if (animal instanceof Bird b) {
            stmt.setNull   (12, Types.BOOLEAN);
            stmt.setNull   (13, Types.VARCHAR);
            stmt.setDouble (14, b.getWingspanCm());
            stmt.setBoolean(15, b.isCanFly());
            stmt.setNull   (16, Types.BOOLEAN);
            stmt.setNull   (17, Types.DOUBLE);
        } else if (animal instanceof Reptile r) {
            stmt.setNull   (12, Types.BOOLEAN);
            stmt.setNull   (13, Types.VARCHAR);
            stmt.setNull   (14, Types.DOUBLE);
            stmt.setNull   (15, Types.BOOLEAN);
            stmt.setBoolean(16, r.isVenomous());
            stmt.setDouble (17, r.getUvbRequirement());
        }
    }
}
