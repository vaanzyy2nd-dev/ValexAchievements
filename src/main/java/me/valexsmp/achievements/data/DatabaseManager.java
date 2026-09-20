package me.valexsmp.achievements.data;

import me.valexsmp.achievements.ValexAchievements;
import me.valexsmp.achievements.player.PlayerAchievementData;

import java.io.File;
import java.sql.*;
import java.util.UUID;

public final class DatabaseManager {
    private final ValexAchievements plugin;
    private Connection connection;

    public DatabaseManager(ValexAchievements plugin) { this.plugin = plugin; }

    public void initialize() {
        try {
            File file = new File(plugin.getDataFolder(), plugin.getConfig().getString("settings.database-file", "data.db"));
            connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
            try (Statement st = connection.createStatement()) {
                st.executeUpdate("CREATE TABLE IF NOT EXISTS players (uuid TEXT PRIMARY KEY, active_title TEXT NOT NULL DEFAULT '')");
                st.executeUpdate("CREATE TABLE IF NOT EXISTS progress (uuid TEXT NOT NULL, achievement_id TEXT NOT NULL, value INTEGER NOT NULL, PRIMARY KEY(uuid, achievement_id))");
                st.executeUpdate("CREATE TABLE IF NOT EXISTS completed (uuid TEXT NOT NULL, achievement_id TEXT NOT NULL, PRIMARY KEY(uuid, achievement_id))");
            }
        } catch (SQLException e) { throw new IllegalStateException("Could not initialize SQLite", e); }
    }

    public synchronized PlayerAchievementData load(UUID uuid) {
        PlayerAchievementData data = new PlayerAchievementData(uuid);
        try {
            try (PreparedStatement ps = connection.prepareStatement("SELECT active_title FROM players WHERE uuid=?")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) { if (rs.next()) data.activeTitle(rs.getString(1)); }
            }
            try (PreparedStatement ps = connection.prepareStatement("SELECT achievement_id,value FROM progress WHERE uuid=?")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) { while (rs.next()) data.setProgress(rs.getString(1), rs.getLong(2)); }
            }
            try (PreparedStatement ps = connection.prepareStatement("SELECT achievement_id FROM completed WHERE uuid=?")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) { while (rs.next()) data.complete(rs.getString(1)); }
            }
        } catch (SQLException e) { plugin.getLogger().warning("Could not load " + uuid + ": " + e.getMessage()); }
        return data;
    }

    public synchronized void save(PlayerAchievementData data) {
        try {
            connection.setAutoCommit(false);
            try (PreparedStatement p = connection.prepareStatement("INSERT INTO players(uuid,active_title) VALUES(?,?) ON CONFLICT(uuid) DO UPDATE SET active_title=excluded.active_title")) {
                p.setString(1, data.uuid().toString()); p.setString(2, data.activeTitle()); p.executeUpdate();
            }
            try (PreparedStatement p = connection.prepareStatement("DELETE FROM progress WHERE uuid=?")) { p.setString(1, data.uuid().toString()); p.executeUpdate(); }
            try (PreparedStatement p = connection.prepareStatement("INSERT INTO progress(uuid,achievement_id,value) VALUES(?,?,?)")) {
                for (var e : data.progress().entrySet()) { p.setString(1, data.uuid().toString()); p.setString(2, e.getKey()); p.setLong(3, e.getValue()); p.addBatch(); }
                p.executeBatch();
            }
            try (PreparedStatement p = connection.prepareStatement("DELETE FROM completed WHERE uuid=?")) { p.setString(1, data.uuid().toString()); p.executeUpdate(); }
            try (PreparedStatement p = connection.prepareStatement("INSERT INTO completed(uuid,achievement_id) VALUES(?,?)")) {
                for (String id : data.completed()) { p.setString(1, data.uuid().toString()); p.setString(2, id); p.addBatch(); }
                p.executeBatch();
            }
            connection.commit(); connection.setAutoCommit(true);
        } catch (SQLException e) {
            try { connection.rollback(); connection.setAutoCommit(true); } catch (SQLException ignored) {}
            plugin.getLogger().warning("Could not save " + data.uuid() + ": " + e.getMessage());
        }
    }

    public void close() { try { if (connection != null) connection.close(); } catch (SQLException ignored) {} }
}
