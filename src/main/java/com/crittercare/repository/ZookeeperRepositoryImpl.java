package com.crittercare.repository;

import com.crittercare.model.Zookeeper;
import com.crittercare.persistence.DatabaseManager;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ZookeeperRepositoryImpl implements ZookeeperRepository {

    private final DatabaseManager dbManager;

    public ZookeeperRepositoryImpl(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public List<Zookeeper> findAll() {
        List<Zookeeper> list = new ArrayList<>();
        try (Statement stmt = dbManager.getConnection().createStatement();
             ResultSet rs   = stmt.executeQuery(
                     "SELECT id, name FROM zookeepers ORDER BY id")) {
            while (rs.next()) {
                list.add(new Zookeeper(rs.getInt("id"), rs.getString("name")));
            }
        } catch (SQLException e) {
            System.err.println("[ZookeeperRepo] findAll error: " + e.getMessage());
        }
        return list;
    }

    @Override
    public Zookeeper save(String name) {
        String sql = "INSERT INTO zookeepers (name) VALUES (?)";
        try (PreparedStatement ps = dbManager.getConnection()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return new Zookeeper(keys.getInt(1), name);
                }
            }
        } catch (SQLException e) {
            System.err.println("[ZookeeperRepo] save error: " + e.getMessage());
        }
        return null;
    }

    @Override
    public boolean rename(int id, String newName) {
        String sql = "UPDATE zookeepers SET name = ? WHERE id = ?";
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, newName);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ZookeeperRepo] rename error: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM zookeepers WHERE id = ?";
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ZookeeperRepo] delete error: " + e.getMessage());
            return false;
        }
    }
}
