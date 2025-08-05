package com.github.queueserver.mohist;

import com.github.queueserver.mohist.config.ConfigManager;
import com.github.queueserver.mohist.database.SimpleDatabaseManager;
import com.github.queueserver.mohist.queue.SimpleQueueManager;
import com.github.queueserver.mohist.commands.QueueCommands;
import com.github.queueserver.mohist.listeners.PlayerListener;
import com.github.queueserver.mohist.listeners.ServerListener;
import com.github.queueserver.mohist.vip.VIPManager;
import com.github.queueserver.mohist.monitor.ServerMonitor;
import com.github.queueserver.mohist.compatibility.ModCompatibilityHandler;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Mohist混合服务器排队插件主类
 * 支持Forge Mod和Bukkit插件的混合环境
 */
public class QueueMohistPlugin extends JavaPlugin {
    
    private ConfigManager configManager;
    private SimpleDatabaseManager databaseManager;
    private SimpleQueueManager queueManager;
    private VIPManager vipManager;
    private ServerMonitor serverMonitor;
    private ModCompatibilityHandler modCompatibilityHandler;
    private volatile boolean serverReady = false;
    private volatile boolean pluginFullyLoaded = false;
    
    @Override
    public void onEnable() {
        getLogger().info("===========================================");
        getLogger().info("  Minecraft Queue Server - Mohist Edition");
        getLogger().info("  版本: " + getDescription().getVersion());
        getLogger().info("  支持: Forge + Bukkit 混合环境");
        getLogger().info("===========================================");
        
        try {
            // 检查Mohist环境
            if (!checkMohistEnvironment()) {
                getLogger().severe("检测到非Mohist环境，插件可能无法正常工作！");
                getLogger().severe("请确保在Mohist服务器上运行此插件");
            }
            
            // 初始化组件
            initializeComponents();
            
            // 注册事件监听器
            registerListeners();
            
            // 注册命令
            registerCommands();
            
            // 启动定时任务
            startScheduledTasks();
            
            // 标记插件完全加载，延迟标记服务器就绪
            pluginFullyLoaded = true;
            
            // 延迟标记服务器就绪，给其他插件和Forge加载时间
            int startupDelay = configManager.getStartupDelay();
            new BukkitRunnable() {
                @Override
                public void run() {
                    serverReady = true;
                    getLogger().info("=== 服务器已准备就绪，可以接受玩家连接 ===");
                }
            }.runTaskLater(this, 20L * startupDelay); // 使用配置中的延迟时间
            
            getLogger().info("队列插件已成功启用！");
            getLogger().info("类型：Mohist混合服务器插件");
            getLogger().info("目标服务器地址：" + configManager.getTargetServerHost());
            
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "插件启用失败！", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    
    @Override
    public void onDisable() {
        getLogger().info("正在关闭队列插件...");
        
        try {
            // 清理资源
            if (queueManager != null) {
                queueManager.clearAllQueues();
            }
            
            if (databaseManager != null) {
                databaseManager.close();
            }
            
            // 取消所有任务
            getServer().getScheduler().cancelTasks(this);
            
            getLogger().info("队列插件已安全关闭");
            
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "插件关闭时发生错误", e);
        }
    }
    
    /**
     * 检查Mohist环境
     */
    private boolean checkMohistEnvironment() {
        try {
            // 检查Mohist相关类
            Class.forName("com.mohistmc.mohist.Mohist");
            getLogger().info("✓ 检测到Mohist环境");
            
            // 检查Forge支持
            try {
                Class.forName("net.minecraftforge.common.MinecraftForge");
                getLogger().info("✓ 检测到Forge环境");
            } catch (ClassNotFoundException e) {
                getLogger().warning("⚠ 未检测到Forge环境，某些功能可能受限");
            }
            
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    /**
     * 初始化所有组件
     */
    private void initializeComponents() {
        getLogger().info("正在初始化组件...");
        
        // 创建配置目录
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        
        // 初始化配置管理器
        configManager = new ConfigManager(new File(getDataFolder(), "config.yml"), getLogger());
        
        // 初始化模组兼容性处理器（优先初始化）
        modCompatibilityHandler = new ModCompatibilityHandler(this);
        getLogger().info("模组兼容性处理器已初始化");
        
        // 初始化数据库管理器
        databaseManager = new SimpleDatabaseManager(this);
        databaseManager.initialize();
        
        // 初始化VIP管理器
        vipManager = new VIPManager(this);
        
        // 初始化队列管理器
        queueManager = new SimpleQueueManager(this);
        
        // 初始化服务器监控器
        serverMonitor = new ServerMonitor(this);
        
        getLogger().info("所有组件初始化完成");
    }
    
    /**
     * 注册事件监听器
     */
    private void registerListeners() {
        getLogger().info("正在注册事件监听器...");
        
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new ServerListener(this), this);
        
        // 注册模组兼容性处理器
        if (modCompatibilityHandler != null) {
            getServer().getPluginManager().registerEvents(modCompatibilityHandler, this);
            getLogger().info("模组兼容性处理器已注册");
        }
        
        getLogger().info("事件监听器注册完成");
    }
    
    /**
     * 注册命令
     */
    private void registerCommands() {
        getLogger().info("正在注册命令...");
        
        QueueCommands queueCommands = new QueueCommands(this);
        
        getCommand("queue").setExecutor(queueCommands);
        getCommand("queueinfo").setExecutor(queueCommands);
        getCommand("leave").setExecutor(queueCommands);
        getCommand("queueadmin").setExecutor(queueCommands);
        getCommand("qstats").setExecutor(queueCommands);
        getCommand("whitelist").setExecutor(queueCommands);
        
        getLogger().info("命令注册完成");
    }
    
    /**
     * 启动定时任务
     */
    private void startScheduledTasks() {
        getLogger().info("正在启动定时任务...");
        
        // VIP缓存清理任务
        new BukkitRunnable() {
            @Override
            public void run() {
                if (vipManager != null) {
                    vipManager.cleanupExpiredCache();
                }
            }
        }.runTaskTimerAsynchronously(this, 20L * 60 * 5, 20L * 60 * 5); // 每5分钟清理一次
        
        // 队列状态更新任务
        new BukkitRunnable() {
            @Override
            public void run() {
                if (queueManager != null && configManager.isQueueEnabled()) {
                    // 记录队列统计
                    SimpleQueueManager.QueueStats stats = queueManager.getQueueStats();
                    databaseManager.recordQueueStats(
                        stats.getTotalSize(),
                        stats.getVipSize(),
                        stats.getTotalSize() // 最大队列大小，这里用当前大小代替
                    );
                }
            }
        }.runTaskTimerAsynchronously(this, 20L * 10, 20L * 300); // 每5分钟记录一次统计
        
        // 队列处理任务 - 这是最重要的任务，负责自动传送队列中的玩家
        new BukkitRunnable() {
            @Override
            public void run() {
                if (queueManager != null && configManager.isQueueEnabled()) {
                    try {
                        processQueuedPlayers();
                    } catch (Exception e) {
                        getLogger().severe("处理队列失败: " + e.getMessage());
                    }
                }
            }
        }.runTaskTimerAsynchronously(this, 20L * configManager.getQueueCheckInterval(), 
                                     20L * configManager.getQueueCheckInterval()); // 根据配置的检查间隔运行
        
        // 启动服务器监控器
        if (serverMonitor != null) {
            serverMonitor.startMonitoring();
        }
        
        getLogger().info("定时任务启动完成");
    }
    
    /**
     * 处理队列中的玩家
     */
    private void processQueuedPlayers() {
        if (!configManager.isQueueServer()) {
            return; // 不是队列服务器，不处理队列
        }
        
        // 检查目标服务器是否有空位
        int maxPlayers = configManager.getMaxPlayers();
        int currentPlayers = getCurrentTargetServerPlayers();
        int availableSlots = maxPlayers - currentPlayers;
        
        if (availableSlots <= 0) {
            return; // 目标服务器已满
        }
        
        // 根据配置的批量传送大小，传送玩家
        int transferBatchSize = Math.min(availableSlots, configManager.getTransferBatchSize());
        
        for (int i = 0; i < transferBatchSize; i++) {
            UUID nextPlayer = queueManager.getNextPlayerInQueue();
            if (nextPlayer == null) {
                break; // 队列为空
            }
            
            // 传送玩家到目标服务器
            transferPlayerToTargetServer(nextPlayer);
        }
    }
    
    /**
     * 获取目标服务器当前玩家数量
     */
    private int getCurrentTargetServerPlayers() {
        if (configManager.isStandaloneMode()) {
            // 独立模式：当前服务器就是目标服务器，返回实际在线玩家数
            return getServer().getOnlinePlayers().size();
        } else {
            // 代理模式：使用服务器监控器获取实际数据
            if (serverMonitor != null && serverMonitor.isTargetServerOnline()) {
                return serverMonitor.getCurrentKnownPlayers();
            }
            return 0; // 服务器离线或监控器未就绪
        }
    }
    
    /**
     * 将玩家传送到目标服务器
     */
    private void transferPlayerToTargetServer(UUID playerId) {
        Player player = getServer().getPlayer(playerId);
        if (player == null || !player.isOnline()) {
            getLogger().warning("尝试传送离线玩家: " + playerId);
            return;
        }
        
        // 在主线程执行传送
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    if (configManager.isStandaloneMode()) {
                        // 独立模式：直接在当前服务器处理
                        handleStandaloneModeTransfer(player);
                    } else {
                        // 代理模式：使用BungeeCord/Velocity传送
                        handleProxyModeTransfer(player);
                    }
                    
                } catch (Exception e) {
                    getLogger().severe("传送玩家失败: " + e.getMessage());
                    player.sendMessage("§c传送失败，请重试或联系管理员");
                }
            }
        }.runTask(this);
    }
    
    /**
     * 处理独立模式下的传送
     */
    private void handleStandaloneModeTransfer(Player player) {
        // 独立模式下，队列服务器和游戏服务器是同一个
        // 只需要给玩家发送消息并移除出队列
        
        player.sendMessage("§a§l恭喜！");
        player.sendMessage("§e您已成功通过队列验证！");
        player.sendMessage("§a现在您可以正常游戏了！");
        player.sendMessage("§7感谢您的耐心等待。");
        
        // 传送到主世界出生点（可选）
        if (getServer().getWorlds().size() > 0) {
            player.teleport(getServer().getWorlds().get(0).getSpawnLocation());
        }
        
        getLogger().info("玩家 " + player.getName() + " 已成功通过队列进入游戏");
    }
    
    /**
     * 处理代理模式下的传送
     */
    private void handleProxyModeTransfer(Player player) {
        String targetServer = configManager.getTargetServer();
        String targetHost = configManager.getTargetServerHost();
        int targetPort = configManager.getTargetServerPort();
        
        player.sendMessage("§a正在传送到游戏服务器...");
        
        // 发送BungeeCord/Velocity传送消息
        if (sendBungeeCordTransfer(player, targetServer)) {
            getLogger().info("玩家 " + player.getName() + " 已通过BungeeCord传送到游戏服务器");
        } else {
            // 如果BungeeCord传送失败，使用踢出方式
            String kickMessage = String.format(
                "§a§l传送到游戏服务器\n\n" +
                "§e请连接到: §f%s:%d\n\n" +
                "§7如果您使用代理服务器,\n" +
                "§7请直接重新连接即可自动进入游戏服务器\n\n" +
                "§a感谢您的等待！",
                targetHost, targetPort
            );
            player.kickPlayer(kickMessage);
            getLogger().info("使用踢出方式传送玩家: " + player.getName());
        }
    }
    
    /**
     * 发送BungeeCord传送消息
     */
    private boolean sendBungeeCordTransfer(Player player, String serverName) {
        try {
            // 注册BungeeCord消息通道（如果尚未注册）
            if (!getServer().getMessenger().isOutgoingChannelRegistered(this, "BungeeCord")) {
                getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
            }
            
            java.io.ByteArrayOutputStream b = new java.io.ByteArrayOutputStream();
            java.io.DataOutputStream out = new java.io.DataOutputStream(b);
            
            out.writeUTF("Connect");
            out.writeUTF(serverName);
            
            player.sendPluginMessage(this, "BungeeCord", b.toByteArray());
            return true;
            
        } catch (Exception e) {
            getLogger().warning("BungeeCord传送失败: " + e.getMessage());
            return false;
        }
    }
    
    // Getter 方法
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public SimpleDatabaseManager getDatabaseManager() {
        return databaseManager;
    }
    
    public SimpleQueueManager getQueueManager() {
        return queueManager;
    }
    
    public VIPManager getVipManager() {
        return vipManager;
    }
    
    public ServerMonitor getServerMonitor() {
        return serverMonitor;
    }
    
    /**
     * 检查服务器是否已就绪
     */
    public boolean isServerReady() {
        return serverReady && pluginFullyLoaded;
    }
    
    /**
     * 检查插件是否完全加载
     */
    public boolean isPluginFullyLoaded() {
        return pluginFullyLoaded;
    }
}
