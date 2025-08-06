package com.github.queueserver.forge;

import com.github.queueserver.forge.config.ConfigManager;
import com.github.queueserver.forge.database.DatabaseManager;
import com.github.queueserver.forge.queue.QueueManager;
import com.github.queueserver.forge.commands.QueueCommands;
import com.github.queueserver.forge.listeners.PlayerConnectionListener;
import com.github.queueserver.forge.listeners.ForgeEventListener;
import com.github.queueserver.forge.vip.VIPManager;
import com.github.queueserver.forge.monitor.ServerMonitor;
import com.github.queueserver.forge.http.ProxyHttpClient;
import com.github.queueserver.forge.compatibility.ForgeModCompatibility;
import com.github.queueserver.forge.security.SecurityManager;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Minecraft 1.20.1 Forge 队列管理插件
 * 使用HTTP协议与代理服务器通信
 * 
 * @author QueueServer Team
 * @version 2.0.0
 * @since 2025-08-06
 */
public class QueueForgePlugin extends JavaPlugin {
    
    // 插件实例
    private static QueueForgePlugin instance;
    
    // 核心管理器
    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private QueueManager queueManager;
    private VIPManager vipManager;
    private ServerMonitor serverMonitor;
    private ProxyHttpClient proxyHttpClient;
    private ForgeModCompatibility forgeCompatibility;
    private SecurityManager securityManager;
    
    // 状态标记
    private volatile boolean serverReady = false;
    private volatile boolean shutdownInProgress = false;
    
    // 定时任务
    private BukkitTask queueProcessTask;
    private BukkitTask heartbeatTask;
    private BukkitTask cleanupTask;
    
    @Override
    public void onEnable() {
        instance = this;
        
        getLogger().info("================================================");
        getLogger().info("  Queue Forge Plugin v2.0.0");
        getLogger().info("  Minecraft 1.20.1 + Forge 47.3.22");
        getLogger().info("  HTTP-based Proxy Communication");
        getLogger().info("================================================");
        
        try {
            // 检查运行环境
            if (!checkEnvironment()) {
                getLogger().severe("环境检查失败，插件将被禁用");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
            
            // 初始化核心组件
            initializeComponents();
            
            // 注册事件监听器
            registerListeners();
            
            // 注册命令
            registerCommands();
            
            // 启动定时任务
            startScheduledTasks();
            
            // 延迟标记服务器就绪
            scheduleServerReady();
            
            getLogger().info("队列插件已成功启用！");
            getLogger().info("代理服务器地址: " + configManager.getProxyServerUrl());
            getLogger().info("队列模式: " + (configManager.isQueueMode() ? "启用" : "禁用"));
            
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "插件启用失败", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    
    @Override
    public void onDisable() {
        shutdownInProgress = true;
        getLogger().info("正在关闭队列插件...");
        
        try {
            // 取消所有定时任务
            cancelTasks();
            
            // 清理队列
            if (queueManager != null) {
                queueManager.shutdown();
            }
            
            // 关闭HTTP客户端
            if (proxyHttpClient != null) {
                proxyHttpClient.shutdown();
            }
            
            // 关闭数据库连接
            if (databaseManager != null) {
                databaseManager.shutdown();
            }
            
            // 清理缓存
            if (vipManager != null) {
                vipManager.shutdown();
            }
            
            getLogger().info("队列插件已安全关闭");
            
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "插件关闭时发生错误", e);
        } finally {
            instance = null;
        }
    }
    
    /**
     * 检查运行环境
     */
    private boolean checkEnvironment() {
        getLogger().info("正在检查运行环境...");
        
        // 检查Java版本
        String javaVersion = System.getProperty("java.version");
        getLogger().info("Java版本: " + javaVersion);
        
        // 检查Bukkit/Paper环境
        String serverVersion = getServer().getVersion();
        getLogger().info("服务器版本: " + serverVersion);
        
        // 检查Forge环境
        boolean forgeDetected = false;
        try {
            Class.forName("net.minecraftforge.common.MinecraftForge");
            forgeDetected = true;
            getLogger().info("✓ Forge环境检测成功");
        } catch (ClassNotFoundException e) {
            getLogger().warning("⚠ 未检测到Forge环境");
        }
        
        // 检查Mohist环境
        boolean mohistDetected = false;
        try {
            Class.forName("com.mohistmc.mohist.Mohist");
            mohistDetected = true;
            getLogger().info("✓ Mohist混合服务器检测成功");
        } catch (ClassNotFoundException e) {
            getLogger().info("ℹ 非Mohist环境");
        }
        
        // 检查Arclight环境
        boolean arclightDetected = false;
        try {
            Class.forName("io.izzel.arclight.common.ArclightMain");
            arclightDetected = true;
            getLogger().info("✓ Arclight混合服务器检测成功");
        } catch (ClassNotFoundException e) {
            getLogger().info("ℹ 非Arclight环境");
        }
        
        if (forgeDetected || mohistDetected || arclightDetected) {
            getLogger().info("✓ 兼容的Forge环境检测成功");
            return true;
        } else {
            getLogger().info("ℹ 标准Bukkit/Paper环境，某些Forge特性将不可用");
            return true; // 允许在非Forge环境下运行
        }
    }
    
    /**
     * 初始化核心组件
     */
    private void initializeComponents() throws Exception {
        getLogger().info("正在初始化核心组件...");
        
        // 创建数据目录
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        
        // 初始化配置管理器
        configManager = new ConfigManager(this);
        configManager.loadConfig();
        getLogger().info("✓ 配置管理器已初始化");
        
        // 初始化安全管理器
        securityManager = new SecurityManager(this);
        getLogger().info("✓ 安全管理器已初始化");
        
        // 初始化HTTP客户端
        proxyHttpClient = new ProxyHttpClient(this);
        getLogger().info("✓ HTTP代理客户端已初始化");
        
        // 初始化数据库管理器
        databaseManager = new DatabaseManager(this);
        databaseManager.initialize();
        getLogger().info("✓ 数据库管理器已初始化");
        
        // 初始化VIP管理器
        vipManager = new VIPManager(this);
        getLogger().info("✓ VIP管理器已初始化");
        
        // 初始化队列管理器
        queueManager = new QueueManager(this);
        getLogger().info("✓ 队列管理器已初始化");
        
        // 初始化服务器监控器
        serverMonitor = new ServerMonitor(this);
        getLogger().info("✓ 服务器监控器已初始化");
        
        // 初始化Forge兼容性处理器
        forgeCompatibility = new ForgeModCompatibility(this);
        getLogger().info("✓ Forge兼容性处理器已初始化");
        
        getLogger().info("所有核心组件初始化完成");
    }
    
    /**
     * 注册事件监听器
     */
    private void registerListeners() {
        getLogger().info("正在注册事件监听器...");
        
        // 玩家连接事件监听器
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        
        // Forge事件监听器
        getServer().getPluginManager().registerEvents(new ForgeEventListener(this), this);
        
        // Forge兼容性事件监听器
        if (forgeCompatibility != null) {
            getServer().getPluginManager().registerEvents(forgeCompatibility, this);
        }
        
        getLogger().info("事件监听器注册完成");
    }
    
    /**
     * 注册命令
     */
    private void registerCommands() {
        getLogger().info("正在注册命令...");
        
        QueueCommands queueCommands = new QueueCommands(this);
        
        // 注册所有队列相关命令
        getCommand("queue").setExecutor(queueCommands);
        getCommand("queueinfo").setExecutor(queueCommands);
        getCommand("leave").setExecutor(queueCommands);
        getCommand("queueadmin").setExecutor(queueCommands);
        getCommand("qstats").setExecutor(queueCommands);
        getCommand("qreload").setExecutor(queueCommands);
        
        getLogger().info("命令注册完成");
    }
    
    /**
     * 启动定时任务
     */
    private void startScheduledTasks() {
        getLogger().info("正在启动定时任务...");
        
        // 队列处理任务
        if (configManager.isQueueMode()) {
            int processInterval = configManager.getQueueProcessInterval();
            queueProcessTask = new BukkitRunnable() {
                @Override
                public void run() {
                    if (!shutdownInProgress && serverReady) {
                        processQueue();
                    }
                }
            }.runTaskTimerAsynchronously(this, 20L * 10, 20L * processInterval);
            getLogger().info("✓ 队列处理任务已启动 (间隔: " + processInterval + "秒)");
        }
        
        // 心跳任务
        int heartbeatInterval = configManager.getHeartbeatInterval();
        heartbeatTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!shutdownInProgress) {
                    sendHeartbeat();
                }
            }
        }.runTaskTimerAsynchronously(this, 20L * 30, 20L * heartbeatInterval);
        getLogger().info("✓ 心跳任务已启动 (间隔: " + heartbeatInterval + "秒)");
        
        // 清理任务
        cleanupTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!shutdownInProgress) {
                    performCleanup();
                }
            }
        }.runTaskTimerAsynchronously(this, 20L * 60 * 5, 20L * 60 * 10); // 每10分钟执行一次
        getLogger().info("✓ 清理任务已启动");
        
        // 启动服务器监控
        serverMonitor.startMonitoring();
        
        getLogger().info("所有定时任务启动完成");
    }
    
    /**
     * 调度服务器就绪状态
     */
    private void scheduleServerReady() {
        int startupDelay = configManager.getStartupDelay();
        
        new BukkitRunnable() {
            @Override
            public void run() {
                serverReady = true;
                getLogger().info("=== 服务器已准备就绪，开始接受玩家请求 ===");
                
                // 通知代理服务器
                proxyHttpClient.notifyServerReady().thenAccept(success -> {
                    if (success) {
                        getLogger().info("已成功通知代理服务器状态");
                    } else {
                        getLogger().warning("通知代理服务器失败");
                    }
                });
            }
        }.runTaskLater(this, 20L * startupDelay);
    }
    
    /**
     * 处理队列
     */
    private void processQueue() {
        try {
            if (queueManager.hasPlayersInQueue()) {
                // 查询目标服务器状态
                proxyHttpClient.getServerStatus().thenAccept(serverStatus -> {
                    if (serverStatus != null && serverStatus.isOnline() && serverStatus.hasAvailableSlots()) {
                        // 计算可传送的玩家数量
                        int availableSlots = serverStatus.getAvailableSlots();
                        int batchSize = Math.min(availableSlots, configManager.getTransferBatchSize());
                        
                        // 传送玩家
                        for (int i = 0; i < batchSize; i++) {
                            UUID nextPlayer = queueManager.getNextPlayer();
                            if (nextPlayer != null) {
                                transferPlayer(nextPlayer);
                            } else {
                                break;
                            }
                        }
                    }
                });
            }
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "处理队列时发生错误", e);
        }
    }
    
    /**
     * 传送玩家
     */
    private void transferPlayer(UUID playerId) {
        Player player = getServer().getPlayer(playerId);
        if (player == null || !player.isOnline()) {
            return;
        }
        
        // 通过HTTP请求传送玩家
        proxyHttpClient.transferPlayer(playerId, player.getName()).thenAccept(success -> {
            if (success) {
                // 在主线程执行玩家操作
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (player.isOnline()) {
                            player.sendMessage("§a§l传送成功！");
                            player.sendMessage("§e正在连接到游戏服务器...");
                            
                            // 记录传送日志
                            getLogger().info("玩家 " + player.getName() + " 已通过队列传送到游戏服务器");
                        }
                    }
                }.runTask(QueueForgePlugin.this);
            } else {
                getLogger().warning("传送玩家失败: " + player.getName());
                player.sendMessage("§c传送失败，请重试或联系管理员");
            }
        });
    }
    
    /**
     * 发送心跳
     */
    private void sendHeartbeat() {
        try {
            proxyHttpClient.sendHeartbeat().thenAccept(success -> {
                if (!success) {
                    getLogger().warning("心跳发送失败，代理服务器可能离线");
                }
            });
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "发送心跳时发生错误", e);
        }
    }
    
    /**
     * 执行清理
     */
    private void performCleanup() {
        try {
            // 清理VIP缓存
            if (vipManager != null) {
                vipManager.cleanup();
            }
            
            // 清理队列中的离线玩家
            if (queueManager != null) {
                queueManager.removeOfflinePlayers();
            }
            
            // 清理数据库连接
            if (databaseManager != null) {
                databaseManager.cleanup();
            }
            
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "执行清理时发生错误", e);
        }
    }
    
    /**
     * 取消所有任务
     */
    private void cancelTasks() {
        if (queueProcessTask != null && !queueProcessTask.isCancelled()) {
            queueProcessTask.cancel();
        }
        if (heartbeatTask != null && !heartbeatTask.isCancelled()) {
            heartbeatTask.cancel();
        }
        if (cleanupTask != null && !cleanupTask.isCancelled()) {
            cleanupTask.cancel();
        }
        
        // 取消所有插件相关任务
        getServer().getScheduler().cancelTasks(this);
    }
    
    // Getter 方法
    public static QueueForgePlugin getInstance() {
        return instance;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
    
    public QueueManager getQueueManager() {
        return queueManager;
    }
    
    public VIPManager getVipManager() {
        return vipManager;
    }
    
    public ServerMonitor getServerMonitor() {
        return serverMonitor;
    }
    
    public ProxyHttpClient getProxyHttpClient() {
        return proxyHttpClient;
    }
    
    public ForgeModCompatibility getForgeCompatibility() {
        return forgeCompatibility;
    }
    
    public SecurityManager getSecurityManager() {
        return securityManager;
    }
    
    /**
     * 检查服务器是否已就绪
     */
    public boolean isServerReady() {
        return serverReady && !shutdownInProgress;
    }
    
    /**
     * 检查是否正在关闭
     */
    public boolean isShuttingDown() {
        return shutdownInProgress;
    }
}
