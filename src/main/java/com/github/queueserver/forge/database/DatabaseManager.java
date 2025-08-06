package com.github.queueserver.forge.database;

import com.github.queueserver.forge.QueueForgePlugin;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 数据库管理器
 * 处理队列数据的持久化存储
 */
public class DatabaseManager {
    
    private final QueueForgePlugin plugin;
    private final Logger logger;
    private HikariDataSource dataSource;
    
    public DatabaseManager(QueueForgePlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }
    
    /**
     * 初始化数据库连接
     */
    public void initialize() throws Exception {
        String databaseType = plugin.getConfigManager().getDatabaseType().toLowerCase();
        
        switch (databaseType) {
            case "sqlite":
                initializeSQLite();
                break;
            case "mysql":
                initializeMySQL();
                break;
            default:
                throw new IllegalArgumentException("不支持的数据库类型: " + databaseType);
        }
        
        // 创建表结构
        createTables();
        
        logger.info("数据库连接已初始化: " + databaseType);
    }
    
    /**
     * 初始化SQLite数据库
     */
    private void initializeSQLite() {
        HikariConfig config = new HikariConfig();
        
        String url = plugin.getConfigManager().getDatabaseUrl();
        if (!url.startsWith("jdbc:sqlite:")) {
            // 如果不是完整URL，构建SQLite文件路径
            String dbPath = plugin.getDataFolder().getAbsolutePath() + "/queue.db";
            url = "jdbc:sqlite:" + dbPath;
        }
        
        config.setJdbcUrl(url);
        config.setDriverClassName("org.sqlite.JDBC");
        config.setMaximumPoolSize(1); // SQLite通常只支持单连接
        config.setConnectionTestQuery("SELECT 1");
        config.setPoolName("QueueForgePlugin-SQLite");
        
        this.dataSource = new HikariDataSource(config);
    }
    
    /**
     * 初始化MySQL数据库
     */
    private void initializeMySQL() {
        HikariConfig config = new HikariConfig();
        
        config.setJdbcUrl(plugin.getConfigManager().getDatabaseUrl());
        config.setUsername(plugin.getConfigManager().getDatabaseUsername());
        config.setPassword(plugin.getConfigManager().getDatabasePassword());
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setMaximumPoolSize(plugin.getConfigManager().getDatabaseMaxPoolSize());
        config.setConnectionTestQuery("SELECT 1");
        config.setPoolName("QueueForgePlugin-MySQL");
        
        // MySQL优化设置
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("useLocalSessionState", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("cacheResultSetMetadata", "true");
        config.addDataSourceProperty("cacheServerConfiguration", "true");
        config.addDataSourceProperty("elideSetAutoCommits", "true");
        config.addDataSourceProperty("maintainTimeStats", "false");
        
        this.dataSource = new HikariDataSource(config);
    }
    
    /**
     * 创建数据库表
     */
    private void createTables() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            // 队列历史表
            String createQueueHistoryTable = """
                CREATE TABLE IF NOT EXISTS queue_history (
                    id INTEGER PRIMARY KEY %s,
                    player_id VARCHAR(36) NOT NULL,
                    player_name VARCHAR(16) NOT NULL,
                    is_vip BOOLEAN NOT NULL,
                    join_time BIGINT NOT NULL,
                    leave_time BIGINT,
                    transfer_time BIGINT,
                    wait_duration BIGINT,
                    status VARCHAR(20) NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """.formatted(plugin.getConfigManager().getDatabaseType().equalsIgnoreCase("sqlite") ? "AUTOINCREMENT" : "AUTO_INCREMENT");
            
            // 队列统计表
            String createQueueStatsTable = """
                CREATE TABLE IF NOT EXISTS queue_stats (
                    id INTEGER PRIMARY KEY %s,
                    date DATE NOT NULL,
                    total_players INTEGER NOT NULL DEFAULT 0,
                    vip_players INTEGER NOT NULL DEFAULT 0,
                    regular_players INTEGER NOT NULL DEFAULT 0,
                    max_queue_size INTEGER NOT NULL DEFAULT 0,
                    average_wait_time BIGINT NOT NULL DEFAULT 0,
                    transfers_completed INTEGER NOT NULL DEFAULT 0,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """.formatted(plugin.getConfigManager().getDatabaseType().equalsIgnoreCase("sqlite") ? "AUTOINCREMENT" : "AUTO_INCREMENT");
            
            // VIP记录表
            String createVipRecordsTable = """
                CREATE TABLE IF NOT EXISTS vip_records (
                    id INTEGER PRIMARY KEY %s,
                    player_id VARCHAR(36) NOT NULL,
                    player_name VARCHAR(16) NOT NULL,
                    is_vip BOOLEAN NOT NULL,
                    last_check TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE(player_id)
                )
                """.formatted(plugin.getConfigManager().getDatabaseType().equalsIgnoreCase("sqlite") ? "AUTOINCREMENT" : "AUTO_INCREMENT");
            
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate(createQueueHistoryTable);
                statement.executeUpdate(createQueueStatsTable);
                statement.executeUpdate(createVipRecordsTable);
                
                logger.info("数据库表结构创建完成");
            }
        }
    }
    
    /**
     * 记录玩家加入队列
     */
    public CompletableFuture<Void> recordPlayerJoinQueue(UUID playerId, String playerName, boolean isVip) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO queue_history (player_id, player_name, is_vip, join_time, status) VALUES (?, ?, ?, ?, ?)";
            
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setString(1, playerId.toString());
                statement.setString(2, playerName);
                statement.setBoolean(3, isVip);
                statement.setLong(4, System.currentTimeMillis());
                statement.setString(5, "QUEUED");
                
                statement.executeUpdate();
                
            } catch (SQLException e) {
                logger.log(Level.WARNING, "记录玩家加入队列失败", e);
            }
        });
    }
    
    /**
     * 记录玩家离开队列
     */
    public CompletableFuture<Void> recordPlayerLeaveQueue(UUID playerId, String reason) {
        return CompletableFuture.runAsync(() -> {
            String sql = "UPDATE queue_history SET leave_time = ?, status = ?, wait_duration = (? - join_time) WHERE player_id = ? AND leave_time IS NULL";
            
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                long currentTime = System.currentTimeMillis();
                statement.setLong(1, currentTime);
                statement.setString(2, reason);
                statement.setLong(3, currentTime);
                statement.setString(4, playerId.toString());
                
                statement.executeUpdate();
                
            } catch (SQLException e) {
                logger.log(Level.WARNING, "记录玩家离开队列失败", e);
            }
        });
    }
    
    /**
     * 记录玩家传送
     */
    public CompletableFuture<Void> recordPlayerTransfer(UUID playerId) {
        return CompletableFuture.runAsync(() -> {
            String sql = "UPDATE queue_history SET transfer_time = ?, status = ?, wait_duration = (? - join_time) WHERE player_id = ? AND transfer_time IS NULL";
            
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                long currentTime = System.currentTimeMillis();
                statement.setLong(1, currentTime);
                statement.setString(2, "TRANSFERRED");
                statement.setLong(3, currentTime);
                statement.setString(4, playerId.toString());
                
                statement.executeUpdate();
                
            } catch (SQLException e) {
                logger.log(Level.WARNING, "记录玩家传送失败", e);
            }
        });
    }
    
    /**
     * 更新VIP记录
     */
    public CompletableFuture<Void> updateVipRecord(UUID playerId, String playerName, boolean isVip) {
        return CompletableFuture.runAsync(() -> {
            String sql = """
                INSERT INTO vip_records (player_id, player_name, is_vip, last_check) 
                VALUES (?, ?, ?, CURRENT_TIMESTAMP)
                ON DUPLICATE KEY UPDATE 
                player_name = VALUES(player_name), 
                is_vip = VALUES(is_vip), 
                last_check = CURRENT_TIMESTAMP
                """;
            
            // SQLite版本
            if (plugin.getConfigManager().getDatabaseType().equalsIgnoreCase("sqlite")) {
                sql = """
                    INSERT OR REPLACE INTO vip_records (player_id, player_name, is_vip, last_check) 
                    VALUES (?, ?, ?, CURRENT_TIMESTAMP)
                    """;
            }
            
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setString(1, playerId.toString());
                statement.setString(2, playerName);
                statement.setBoolean(3, isVip);
                
                statement.executeUpdate();
                
            } catch (SQLException e) {
                logger.log(Level.WARNING, "更新VIP记录失败", e);
            }
        });
    }
    
    /**
     * 记录队列统计
     */
    public CompletableFuture<Void> recordQueueStats(int totalPlayers, int vipPlayers, int maxQueueSize) {
        return CompletableFuture.runAsync(() -> {
            String sql = """
                INSERT INTO queue_stats (date, total_players, vip_players, regular_players, max_queue_size, transfers_completed, updated_at) 
                VALUES (CURRENT_DATE, ?, ?, ?, ?, 0, CURRENT_TIMESTAMP)
                ON DUPLICATE KEY UPDATE 
                total_players = GREATEST(total_players, VALUES(total_players)),
                vip_players = GREATEST(vip_players, VALUES(vip_players)),
                regular_players = GREATEST(regular_players, VALUES(regular_players)),
                max_queue_size = GREATEST(max_queue_size, VALUES(max_queue_size)),
                updated_at = CURRENT_TIMESTAMP
                """;
            
            // SQLite版本
            if (plugin.getConfigManager().getDatabaseType().equalsIgnoreCase("sqlite")) {
                sql = """
                    INSERT OR REPLACE INTO queue_stats (date, total_players, vip_players, regular_players, max_queue_size, transfers_completed) 
                    VALUES (date('now'), ?, ?, ?, ?, 0)
                    """;
            }
            
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setInt(1, totalPlayers);
                statement.setInt(2, vipPlayers);
                statement.setInt(3, totalPlayers - vipPlayers);
                statement.setInt(4, maxQueueSize);
                
                statement.executeUpdate();
                
            } catch (SQLException e) {
                logger.log(Level.WARNING, "记录队列统计失败", e);
            }
        });
    }
    
    /**
     * 清理过期数据
     */
    public void cleanup() {
        CompletableFuture.runAsync(() -> {
            try (Connection connection = dataSource.getConnection()) {
                // 清理7天前的队列历史
                String cleanupHistory = "DELETE FROM queue_history WHERE created_at < datetime('now', '-7 days')";
                if (!plugin.getConfigManager().getDatabaseType().equalsIgnoreCase("sqlite")) {
                    cleanupHistory = "DELETE FROM queue_history WHERE created_at < DATE_SUB(NOW(), INTERVAL 7 DAY)";
                }
                
                try (Statement statement = connection.createStatement()) {
                    int deleted = statement.executeUpdate(cleanupHistory);
                    if (deleted > 0) {
                        logger.info("清理了 " + deleted + " 条过期队列历史记录");
                    }
                }
                
            } catch (SQLException e) {
                logger.log(Level.WARNING, "清理数据库失败", e);
            }
        });
    }
    
    /**
     * 关闭数据库连接
     */
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("数据库连接已关闭");
        }
    }
    
    /**
     * 获取数据源
     */
    public HikariDataSource getDataSource() {
        return dataSource;
    }
}
