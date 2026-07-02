package com.crittercare.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Manages the application's primary database connection lifecycle.
 * <p>
 * Implements a thread-safe singleton pattern to maintain a single active
 * JDBC connection to the underlying embedded H2 database, ensuring centralized
 * and consistent data access across all repositories.
 * </p>
 */
public class DatabaseManager {

    // File-based H2 database stored in ./data/crittercare.mv.db
    private static final String DB_URL  = "jdbc:h2:file:./data/crittercare;AUTO_SERVER=FALSE";
    private static final String DB_USER = "sa";
    private static final String DB_PASS = "";

    private static DatabaseManager instance;
    private Connection connection;

    /**
     * Private constructor to prevent external instantiation.
     * Automatically establishes the initial database connection upon creation.
     */
    private DatabaseManager() {
        openConnection();
    }

    /**
     * Retrieves the singleton instance of the database manager.
     *
     * @return the synchronized, active instance of the {@code DatabaseManager}
     */
    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    /**
     * Retrieves the active database connection.
     * <p>
     * Automatically attempts to re-establish the connection if it has been
     * inadvertently closed or dropped.
     * </p>
     *
     * @return the active JDBC {@link Connection}
     * @throws RuntimeException if the connection state cannot be verified
     */
    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                openConnection();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not verify H2 connection state.", e);
        }
        return connection;
    }

    /**
     * Gracefully terminates the active database connection.
     * <p>
     * This method must be invoked during the application shutdown sequence
     * to release resources and prevent database lock issues.
     * </p>
     */
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("[DB] Connection closed.");
            }
        } catch (SQLException e) {
            System.err.println("[DB] Error closing connection: " + e.getMessage());
        }
    }

    /**
     * Initializes the JDBC connection using the configured driver and credentials.
     *
     * @throws RuntimeException if the JDBC driver is missing or the connection fails
     */
    private void openConnection() {
        try {
            // Ensure H2 driver is loaded (needed in some fat-jar setups)
            Class.forName("org.h2.Driver");
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
            System.out.println("[DB] H2 connection established → " + DB_URL);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("H2 Driver not found on classpath.", e);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to open H2 connection.", e);
        }
    }
}
