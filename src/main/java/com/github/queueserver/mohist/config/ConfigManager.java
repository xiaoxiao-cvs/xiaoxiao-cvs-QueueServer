package com.github.queueserver.mohist.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Mohist配置管理器
 */
public class ConfigManager {
    
    private final File configFile;
    private FileConfiguration config;
    private final Logger logger;
    
    public ConfigManager(File configFile, Logger logger) {
        this.configFile = configFile;
        this.logger = logger;
        loadConfig();
    }
    
    /**
     * 加载配置文件
     */
    public void loadConfig() {
        try {
            if (!configFile.exists()) {
                createDefaultConfig();
            }
            
            config = YamlConfiguration.loadConfiguration(configFile);
            logger.info("配置文件已加载: " + configFile.getName());
            
        } catch (Exception e) {
            logger.severe("加载配置文件失败: " + e.getMessage());
            createDefaultConfig();
        }
    }
    
    /**
     * 重载配置文件
     */
    public void reloadConfig() {
        loadConfig();
    }
    
    /**
     * 创建默认配置
     */
    private void createDefaultConfig() {
        try {
            configFile.getParentFile().mkdirs();
            config = new YamlConfiguration();
            
            // 服务器设置
            config.set("server.queue-server", true);
            config.set("server.target-server", "survival");
            config.set("server.target-server-host", "127.0.0.1");
            config.set("server.target-server-port", 25565);
            config.set("server.max-players", 50);
            
            // 队列设置
            config.set("queue.enabled", true);
            config.set("queue.check-interval", 30);
            config.set("queue.transfer-batch-size", 5);
            
            // 服务器监控设置
            config.set("monitor.enabled", true);
            config.set("monitor.check-interval", 30);
            config.set("monitor.broadcast-interval", 300);
            config.set("monitor.connection-timeout", 5000);
            config.set("monitor.broadcast-on-change", true);
            config.set("monitor.messages.standalone-format", 
                "§6§l[服务器状态] §a当前在线: §e{current}§7/{max} §8| §a队列中: §e{queue}人");
            config.set("monitor.messages.proxy-format", 
                "§6§l[服务器状态] §a{target}服务器在线: §e{current}人 §8| §a队列服务器: §e{queue}人排队");
            config.set("monitor.messages.offline-format", 
                "§c§l[服务器状态] §c{target}服务器暂时离线，请稍后再试");
            
            // VIP设置
            config.set("vip.enabled", true);
            config.set("vip.priority-levels.bronze", 1);
            config.set("vip.priority-levels.silver", 2);
            config.set("vip.priority-levels.gold", 3);
            config.set("vip.priority-levels.diamond", 4);
            config.set("vip.priority-levels.supreme", 5);
            
            // 白名单设置
            config.set("whitelist.enabled", false);
            config.set("whitelist.message", "§c您不在服务器白名单中！");
            
            // 数据库设置
            config.set("database.type", "sqlite");
            config.set("database.sqlite.file", "queue_data.db");
            
            config.save(configFile);
            logger.info("已创建默认配置文件");
            
        } catch (Exception e) {
            logger.severe("创建默认配置失败: " + e.getMessage());
        }
    }
    
    // Getter 方法
    public boolean isQueueServer() {
        return config.getBoolean("server.queue-server", true);
    }
    
    public String getTargetServer() {
        return config.getString("server.target-server", "survival");
    }
    
    public String getTargetServerHost() {
        return config.getString("server.target-server-host", "127.0.0.1");
    }
    
    public int getTargetServerPort() {
        return config.getInt("server.target-server-port", 25565);
    }
    
    public int getMaxPlayers() {
        return config.getInt("server.max-players", 50);
    }
    
    public boolean isQueueEnabled() {
        return config.getBoolean("queue.enabled", true);
    }
    
    public int getCheckInterval() {
        return config.getInt("queue.check-interval", 30);
    }
    
    public int getQueueCheckInterval() {
        return getCheckInterval(); // 使用相同的检查间隔
    }
    
    public int getTransferBatchSize() {
        return config.getInt("queue.transfer-batch-size", 5);
    }
    
    public boolean isStandaloneMode() {
        return config.getBoolean("server.standalone-mode", false);
    }
    
    public boolean isVipEnabled() {
        return config.getBoolean("vip.enabled", true);
    }
    
    public Map<String, Integer> getVipPriorityLevels() {
        Map<String, Integer> levels = new HashMap<>();
        
        if (config.getConfigurationSection("vip.priority-levels") != null) {
            for (String key : config.getConfigurationSection("vip.priority-levels").getKeys(false)) {
                levels.put(key, config.getInt("vip.priority-levels." + key));
            }
        }
        
        return levels;
    }
    
    public boolean isWhitelistEnabled() {
        return config.getBoolean("whitelist.enabled", false);
    }
    
    public String getWhitelistMessage() {
        return config.getString("whitelist.message", "§c您不在服务器白名单中！");
    }
    
    // 服务器监控相关配置
    public boolean isMonitorEnabled() {
        return config.getBoolean("monitor.enabled", true);
    }
    
    public int getMonitorCheckInterval() {
        return config.getInt("monitor.check-interval", 30);
    }
    
    public int getMonitorBroadcastInterval() {
        return config.getInt("monitor.broadcast-interval", 300);
    }
    
    public int getMonitorConnectionTimeout() {
        return config.getInt("monitor.connection-timeout", 5000);
    }
    
    public boolean isBroadcastOnChange() {
        return config.getBoolean("monitor.broadcast-on-change", true);
    }
    
    public String getStandaloneFormatMessage() {
        return config.getString("monitor.messages.standalone-format", 
            "§6§l[服务器状态] §a当前在线: §e{current}§7/{max} §8| §a队列中: §e{queue}人");
    }
    
    public String getProxyFormatMessage() {
        return config.getString("monitor.messages.proxy-format", 
            "§6§l[服务器状态] §a{target}服务器在线: §e{current}人 §8| §a队列服务器: §e{queue}人排队");
    }
    
    public String getOfflineFormatMessage() {
        return config.getString("monitor.messages.offline-format", 
            "§c§l[服务器状态] §c{target}服务器暂时离线，请稍后再试");
    }
    
    public String getDatabaseType() {
        return config.getString("database.type", "sqlite");
    }
    
    public String getSqliteFile() {
        return config.getString("database.sqlite.file", "queue_data.db");
    }
    
    public FileConfiguration getConfig() {
        return config;
    }
}
