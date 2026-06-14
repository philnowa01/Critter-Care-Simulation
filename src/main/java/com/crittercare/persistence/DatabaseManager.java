package com.crittercare.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Singleton that owns the application's single H2 JDBC connection.
 *
 * Using a singleton here is appropriate because:
 *  - H2 in embedded mode supports only one JVM connection at a time.
 *  - All DB access happens on one thread (JavaFX Application Thread or
 *    the simulation executor, which serializes its own writes).
 *
 * Call getInstance().getConnection() from any repository.
 * Call closeConnection() once in MainApp.stop().
 */
public class DatabaseManager {

    // File-based H2 database stored in ./data/crittercare.mv.db
    private static final String DB_URL  = "jdbc:h2:file:./data/crittercare;AUTO_SERVER=FALSE";
    private static final String DB_USER = "sa";
    private static final String DB_PASS = "";

    private static DatabaseManager instance;
    private Connection connection;

    // ── Constructor ──────────────────────────────────────────────────────────

    private DatabaseManager() {
        openConnection();
    }

    // ── Singleton accessor ───────────────────────────────────────────────────

    /**
     * Returns the single DatabaseManager instance, creating it on first call.
     * Synchronized to be safe even if two threads race during startup.
     */
    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    // ── Public API ───────────────────────────────────────────────────────────

    /**
     * Returns the live Connection, reopening it automatically if it was
     * closed (e.g. after a test teardown).
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
     * Gracefully closes the connection.  Call this once from MainApp.stop().
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

    // ── Private helpers ──────────────────────────────────────────────────────

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
