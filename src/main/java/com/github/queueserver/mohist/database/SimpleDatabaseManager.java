package com.github.queueserver.mohist.database;

import com.github.queueserver.mohist.QueueMohistPlugin;

import java.io.File;
import java.sql.*;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * 简化的数据库管理器
 */
public class SimpleDatabaseManager {
    
    private final QueueMohistPlugin plugin;
    private final Logger logger;
    private Connection connection;
    private final String databaseFile;
    
    public SimpleDatabaseManager(QueueMohistPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.databaseFile = new File(plugin.getDataFolder(), "queue_data.db").getAbsolutePath();
    }
    
    /**
     * 初始化数据库
     */
    public void initialize() {
        try {
            // 创建数据目录
            plugin.getDataFolder().mkdirs();
            
            // 连接到SQLite数据库
            String url = "jdbc:sqlite:" + databaseFile;
            connection = DriverManager.getConnection(url);
            
            logger.info("已连接到SQLite数据库: " + databaseFile);
            
            // 创建表
            createTables();
            
        } catch (SQLException e) {
            logger.severe("数据库初始化失败: " + e.getMessage());
        }
    }
    
    /**
     * 创建数据表
     */
    private void createTables() throws SQLException {
        String[] sqlStatements = {
            // VIP数据表
            """
            CREATE TABLE IF NOT EXISTS vip_players (
                player_id TEXT PRIMARY KEY,
                vip_level INTEGER NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """,
            
            // 白名单表
            """
            CREATE TABLE IF NOT EXISTS whitelist (
                player_id TEXT PRIMARY KEY,
                player_name TEXT,
                added_by TEXT,
                added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """,
            
            // 队列统计表
            """
            CREATE TABLE IF NOT EXISTS queue_stats (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                date TEXT,
                total_players INTEGER,
                vip_players INTEGER,
                max_queue_size INTEGER,
                recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """
        };
        
        for (String sql : sqlStatements) {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute(sql);
            }
        }
        
        logger.info("数据库表结构已创建");
    }
    
    /**
     * 更新VIP状态
     */
    public void updateVIPStatus(UUID playerId, int vipLevel) {
        String sql = """
            INSERT OR REPLACE INTO vip_players (player_id, vip_level, updated_at)
            VALUES (?, ?, CURRENT_TIMESTAMP)
            """;
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerId.toString());
            pstmt.setInt(2, vipLevel);
            pstmt.executeUpdate();
            
            logger.info("VIP状态已更新: " + playerId + " -> " + vipLevel);
            
        } catch (SQLException e) {
            logger.severe("更新VIP状态失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取VIP等级
     */
    public int getVIPLevel(UUID playerId) {
        String sql = "SELECT vip_level FROM vip_players WHERE player_id = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerId.toString());
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("vip_level");
                }
            }
            
        } catch (SQLException e) {
            logger.severe("获取VIP等级失败: " + e.getMessage());
        }
        
        return 0; // 默认不是VIP
    }
    
    /**
     * 检查玩家是否在白名单中
     */
    public boolean isPlayerWhitelisted(UUID playerId) {
        String sql = "SELECT 1 FROM whitelist WHERE player_id = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerId.toString());
            
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
            
        } catch (SQLException e) {
            logger.severe("检查白名单失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 添加玩家到白名单
     */
    public void addToWhitelist(UUID playerId, String playerName, String addedBy) {
        String sql = """
            INSERT OR REPLACE INTO whitelist (player_id, player_name, added_by)
            VALUES (?, ?, ?)
            """;
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerId.toString());
            pstmt.setString(2, playerName);
            pstmt.setString(3, addedBy);
            pstmt.executeUpdate();
            
            logger.info("玩家已添加到白名单: " + playerName + " (by " + addedBy + ")");
            
        } catch (SQLException e) {
            logger.severe("添加白名单失败: " + e.getMessage());
        }
    }
    
    /**
     * 从白名单移除玩家
     */
    public void removeFromWhitelist(UUID playerId) {
        String sql = "DELETE FROM whitelist WHERE player_id = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerId.toString());
            int affected = pstmt.executeUpdate();
            
            if (affected > 0) {
                logger.info("玩家已从白名单移除: " + playerId);
            }
            
        } catch (SQLException e) {
            logger.severe("移除白名单失败: " + e.getMessage());
        }
    }
    
    /**
     * 记录队列统计
     */
    public void recordQueueStats(int totalPlayers, int vipPlayers, int maxQueueSize) {
        String sql = """
            INSERT INTO queue_stats (date, total_players, vip_players, max_queue_size)
            VALUES (date('now'), ?, ?, ?)
            """;
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, totalPlayers);
            pstmt.setInt(2, vipPlayers);
            pstmt.setInt(3, maxQueueSize);
            pstmt.executeUpdate();
            
        } catch (SQLException e) {
            logger.warning("记录队列统计失败: " + e.getMessage());
        }
    }
    
    /**
     * 关闭数据库连接
     */
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                logger.info("数据库连接已关闭");
            }
        } catch (SQLException e) {
            logger.severe("关闭数据库连接失败: " + e.getMessage());
        }
    }
    
    /**
     * 检查数据库连接
     */
    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
}
