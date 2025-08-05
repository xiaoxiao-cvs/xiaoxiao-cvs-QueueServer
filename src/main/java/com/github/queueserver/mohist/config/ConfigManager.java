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
            config.set("server.standalone-mode", false);
            config.set("server.proxy-mode", true);
            config.set("server.proxy-type", "velocity");
            config.set("server.ip-forwarding", true);
            
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
            
            // 客户端兼容性设置
            config.set("client-compatibility.allow-vanilla-clients", true);
            config.set("client-compatibility.allow-forge-clients", true);
            config.set("client-compatibility.detect-client-type", true);
            config.set("client-compatibility.force-client-type", "none");
            config.set("client-compatibility.type-mismatch-action", "warn");
            
            // Velocity API设置
            config.set("velocity-api.enabled", true);
            config.set("velocity-api.host", "127.0.0.1");
            config.set("velocity-api.port", 8080);
            config.set("velocity-api.secret", "your-api-secret-here");
            config.set("velocity-api.use-web-api", true);
            config.set("velocity-api.fallback-to-kick", true);
            config.set("velocity-api.connection-timeout", 5000);
            config.set("velocity-api.read-timeout", 10000);
            
            // 服务器映射设置
            config.set("servers.survival.host", "8.138.197.208");
            config.set("servers.survival.port", 25565);
            config.set("servers.creative.host", "8.138.197.208");
            config.set("servers.creative.port", 25566);
            
            // Mohist 特定设置
            config.set("mohist.forge-compatibility", true);
            config.set("mohist.bukkit-permissions", true);
            config.set("mohist.vault-integration", true);
            
            // 消息设置
            config.set("messages.client-type-detected", "&a检测到客户端类型: &e{type}");
            config.set("messages.client-incompatible", "&c警告: 你的客户端可能与目标服务器不兼容");
            
            // 日志设置
            config.set("logging.log-client-connections", true);
            
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
    
    public boolean isProxyMode() {
        return config.getBoolean("server.proxy-mode", true);
    }
    
    public String getProxyType() {
        return config.getString("server.proxy-type", "velocity");
    }
    
    public boolean isIpForwardingEnabled() {
        return config.getBoolean("server.ip-forwarding", true);
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
    
    // 客户端兼容性配置方法
    public boolean isVanillaClientsAllowed() {
        return config.getBoolean("client-compatibility.allow-vanilla-clients", true);
    }
    
    public boolean isForgeClientsAllowed() {
        return config.getBoolean("client-compatibility.allow-forge-clients", true);
    }
    
    public boolean isClientTypeDetectionEnabled() {
        return config.getBoolean("client-compatibility.detect-client-type", true);
    }
    
    public String getForcedClientType() {
        return config.getString("client-compatibility.force-client-type", "none");
    }
    
    public String getTypeMismatchAction() {
        return config.getString("client-compatibility.type-mismatch-action", "warn");
    }
    
    public String getClientTypeDetectedMessage() {
        return config.getString("messages.client-type-detected", "&a检测到客户端类型: &e{type}");
    }
    
    public String getClientIncompatibleMessage() {
        return config.getString("messages.client-incompatible", "&c警告: 你的客户端可能与目标服务器不兼容");
    }
    
    public boolean isClientConnectionLoggingEnabled() {
        return config.getBoolean("logging.log-client-connections", true);
    }
    
    // Mohist 特定配置方法
    public boolean isForgeCompatibilityEnabled() {
        return config.getBoolean("mohist.forge-compatibility", true);
    }
    
    public boolean isBukkitPermissionsEnabled() {
        return config.getBoolean("mohist.bukkit-permissions", true);
    }
    
    public boolean isVaultIntegrationEnabled() {
        return config.getBoolean("mohist.vault-integration", true);
    }
    
    /**
     * 获取服务器启动延迟时间（秒）
     */
    public int getStartupDelay() {
        return config.getInt("mohist.startup-delay", 10);
    }
    
    /**
     * 检查是否启用客户端连接日志（Mohist配置）
     */
    public boolean isMohistClientConnectionLoggingEnabled() {
        return config.getBoolean("mohist.client-connection-logging", true);
    }
    
    /**
     * 检查是否启用客户端类型检测（Mohist配置）
     */
    public boolean isMohistClientTypeDetectionEnabled() {
        return config.getBoolean("mohist.client-type-detection", true);
    }
    
    // Velocity API 配置方法
    public boolean isVelocityApiEnabled() {
        return config.getBoolean("velocity-api.enabled", true);
    }
    
    public String getVelocityApiHost() {
        return config.getString("velocity-api.host", "127.0.0.1");
    }
    
    public int getVelocityApiPort() {
        return config.getInt("velocity-api.port", 8080);
    }
    
    public String getVelocityApiSecret() {
        return config.getString("velocity-api.secret", "your-api-secret-here");
    }
    
    public boolean isVelocityWebApiEnabled() {
        return config.getBoolean("velocity-api.use-web-api", true);
    }
    
    public boolean isVelocityFallbackToKickEnabled() {
        return config.getBoolean("velocity-api.fallback-to-kick", true);
    }
    
    public int getVelocityConnectionTimeout() {
        return config.getInt("velocity-api.connection-timeout", 5000);
    }
    
    public int getVelocityReadTimeout() {
        return config.getInt("velocity-api.read-timeout", 10000);
    }
    
    // 服务器映射配置方法
    public String getServerHost(String serverName) {
        return config.getString("servers." + serverName + ".host");
    }
    
    public int getServerPort(String serverName) {
        return config.getInt("servers." + serverName + ".port", 25565);
    }

    public FileConfiguration getConfig() {
        return config;
    }
}
