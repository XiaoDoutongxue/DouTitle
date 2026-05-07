package org.doutitle.database;

import org.doutitle.DouTitle;
import org.bukkit.entity.Player;

import java.sql.*;
import java.util.*;

public class DatabaseManager {

    private final DouTitle plugin;
    private Connection connection;

    public DatabaseManager(DouTitle plugin) {
        this.plugin = plugin;
        initDatabase();
    }

    private void initDatabase() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:plugins/DouTitle/data.db");

            // 创建玩家称号数据表
            String createPlayerTitles = "CREATE TABLE IF NOT EXISTS player_titles (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "player_uuid VARCHAR(36) NOT NULL," +
                    "title_id VARCHAR(64) NOT NULL," +
                    "obtain_time LONG NOT NULL," +
                    "expire_time LONG NOT NULL," +
                    "UNIQUE(player_uuid, title_id)" +
                    ")";

            // 创建玩家佩戴称号表
            String createPlayerEquip = "CREATE TABLE IF NOT EXISTS player_equip (" +
                    "player_uuid VARCHAR(36) PRIMARY KEY," +
                    "title_id VARCHAR(64)" +
                    ")";

            // 创建称号进度表
            String createProgress = "CREATE TABLE IF NOT EXISTS title_progress (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "player_uuid VARCHAR(36) NOT NULL," +
                    "title_id VARCHAR(64) NOT NULL," +
                    "progress_type VARCHAR(32) NOT NULL," +
                    "current_amount LONG DEFAULT 0," +
                    "UNIQUE(player_uuid, title_id, progress_type)" +
                    ")";

            try (Statement stmt = connection.createStatement()) {
                stmt.execute(createPlayerTitles);
                stmt.execute(createPlayerEquip);
                stmt.execute(createProgress);
            }

        } catch (SQLException | ClassNotFoundException e) {
            plugin.getLogger().severe("数据库初始化失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ========== 玩家称号操作 ==========

    /**
     * 给予玩家称号
     */
    public void givePlayerTitle(UUID uuid, String titleId, long duration) {
        long obtainTime = System.currentTimeMillis();
        long expireTime = duration == -1 ? -1 : obtainTime + (duration * 1000);

        String sql = "INSERT OR REPLACE INTO player_titles (player_uuid, title_id, obtain_time, expire_time) VALUES (?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            pstmt.setString(2, titleId);
            pstmt.setLong(3, obtainTime);
            pstmt.setLong(4, expireTime);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 移除玩家称号
     */
    public void removePlayerTitle(UUID uuid, String titleId) {
        String sql = "DELETE FROM player_titles WHERE player_uuid = ? AND title_id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            pstmt.setString(2, titleId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取玩家拥有的所有称号ID
     */
    public List<String> getPlayerTitles(UUID uuid) {
        List<String> titles = new ArrayList<>();
        String sql = "SELECT title_id FROM player_titles WHERE player_uuid = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                titles.add(rs.getString("title_id"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return titles;
    }

    /**
     * 获取玩家称号的过期时间
     */
    public long getTitleExpireTime(UUID uuid, String titleId) {
        String sql = "SELECT expire_time FROM player_titles WHERE player_uuid = ? AND title_id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            pstmt.setString(2, titleId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getLong("expire_time");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * 检查玩家是否拥有称号
     */
    public boolean hasPlayerTitle(UUID uuid, String titleId) {
        String sql = "SELECT 1 FROM player_titles WHERE player_uuid = ? AND title_id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            pstmt.setString(2, titleId);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // ========== 佩戴称号操作 ==========

    /**
     * 设置玩家佩戴的称号
     */
    public void setEquippedTitle(UUID uuid, String titleId) {
        String sql = "INSERT OR REPLACE INTO player_equip (player_uuid, title_id) VALUES (?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            pstmt.setString(2, titleId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取玩家佩戴的称号
     */
    public String getEquippedTitle(UUID uuid) {
        String sql = "SELECT title_id FROM player_equip WHERE player_uuid = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("title_id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 卸下玩家称号
     */
    public void removeEquippedTitle(UUID uuid) {
        String sql = "DELETE FROM player_equip WHERE player_uuid = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ========== 称号进度操作 ==========

    /**
     * 增加进度
     */
    public void addProgress(UUID uuid, String titleId, String type, long amount) {
        String sql = "INSERT INTO title_progress (player_uuid, title_id, progress_type, current_amount) " +
                "VALUES (?, ?, ?, ?) " +
                "ON CONFLICT(player_uuid, title_id, progress_type) DO UPDATE SET " +
                "current_amount = current_amount + ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            pstmt.setString(2, titleId);
            pstmt.setString(3, type);
            pstmt.setLong(4, amount);
            pstmt.setLong(5, amount);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取进度
     */
    public long getProgress(UUID uuid, String titleId, String type) {
        String sql = "SELECT current_amount FROM title_progress WHERE player_uuid = ? AND title_id = ? AND progress_type = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            pstmt.setString(2, titleId);
            pstmt.setString(3, type);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getLong("current_amount");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * 设置进度
     */
    public void setProgress(UUID uuid, String titleId, String type, long amount) {
        String sql = "INSERT OR REPLACE INTO title_progress (player_uuid, title_id, progress_type, current_amount) VALUES (?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            pstmt.setString(2, titleId);
            pstmt.setString(3, type);
            pstmt.setLong(4, amount);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 检查并清除过期称号
    public void checkAndCleanExpiredTitles() {
        String sql = "DELETE FROM player_titles WHERE expire_time != -1 AND expire_time <= ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, System.currentTimeMillis());
            int deleted = pstmt.executeUpdate();
            if (deleted > 0) {
                plugin.getLogger().info("已清除 " + deleted + " 个过期称号");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}