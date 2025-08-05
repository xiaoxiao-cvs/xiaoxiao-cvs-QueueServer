package com.github.queueserver.mohist.listeners;

import com.github.queueserver.mohist.QueueMohistPlugin;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

/**
 * Mohist环境下的玩家事件监听器
 */
public class PlayerListener implements Listener {
    
    private final QueueMohistPlugin plugin;
    
    public PlayerListener(QueueMohistPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * 玩家登录事件
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        try {
            // 检查服务器是否为队列服务器
            if (!isQueueServer()) {
                return; // 不是队列服务器，允许正常登录
            }
            
            // 检查代理模式下的IP转发
            if (plugin.getConfigManager().isProxyMode() && plugin.getConfigManager().isIpForwardingEnabled()) {
                if (!validateProxyForwarding(player, event)) {
                    return; // 如果验证失败，事件已被处理
                }
            }
            
            // 检查服务器是否已准备就绪
            if (!plugin.isServerReady()) {
                event.disallow(PlayerLoginEvent.Result.KICK_OTHER, 
                    "§c§l服务器正在启动中...\n§e请稍等片刻后重新连接\n§7预计启动时间: 30-60秒");
                plugin.getLogger().info("玩家 " + player.getName() + " 在服务器启动期间尝试连接，已拒绝");
                return;
            }
            
            // 记录客户端类型信息（用于调试）
            logClientInfo(player);
            
            // 检查白名单
            if (plugin.getConfigManager().isWhitelistEnabled()) {
                if (!plugin.getDatabaseManager().isPlayerWhitelisted(playerId)) {
                    event.disallow(PlayerLoginEvent.Result.KICK_WHITELIST, 
                        "§c您不在服务器白名单中！\n§e请联系管理员申请白名单");
                    return;
                }
            }
            
            // 检查服务器是否已满
            if (isServerFull() && !isVIPPlayer(player)) {
                // 非VIP玩家且服务器已满，拒绝登录
                event.disallow(PlayerLoginEvent.Result.KICK_FULL, 
                    "§c服务器已满！\n§e您已加入排队队列\n§a请等待空位...");
                return;
            }
            
            plugin.getLogger().info("玩家 " + player.getName() + " 登录到队列服务器");
            
        } catch (Exception e) {
            plugin.getLogger().severe("处理玩家登录事件失败: " + e.getMessage());
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, "§c服务器内部错误，请稍后重试");
        }
    }
    
    /**
     * 玩家加入事件
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        // 延迟处理，确保玩家完全加载
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    // 检查是否为队列服务器
                    if (!isQueueServer()) {
                        return;
                    }
                    
                    // 将玩家加入队列
                    addPlayerToQueue(player);
                    
                    // 发送欢迎消息
                    sendWelcomeMessage(player);
                    
                    // 显示队列状态
                    showQueueStatus(player);
                    
                } catch (Exception e) {
                    plugin.getLogger().severe("处理玩家加入事件失败: " + e.getMessage());
                }
            }
        }.runTaskLater(plugin, 20L); // 延迟1秒执行
    }
    
    /**
     * 玩家离开事件
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        try {
            // 从队列中移除玩家
            plugin.getQueueManager().removeFromQueue(playerId);
            
            plugin.getLogger().info("玩家 " + player.getName() + " 离开队列服务器");
            
        } catch (Exception e) {
            plugin.getLogger().severe("处理玩家离开事件失败: " + e.getMessage());
        }
    }
    
    /**
     * 检查是否为队列服务器
     */
    private boolean isQueueServer() {
        return plugin.getConfigManager().isQueueServer();
    }
    
    /**
     * 检查服务器是否已满
     */
    private boolean isServerFull() {
        int maxPlayers = plugin.getConfigManager().getMaxPlayers();
        int currentPlayers = plugin.getServer().getOnlinePlayers().size();
        return currentPlayers >= maxPlayers;
    }
    
    /**
     * 检查是否为VIP玩家
     */
    private boolean isVIPPlayer(Player player) {
        return plugin.getVipManager().isVIP(player);
    }
    
    /**
     * 将玩家加入队列
     */
    private void addPlayerToQueue(Player player) {
        UUID playerId = player.getUniqueId();
        
        // 检查是否已在队列中
        if (plugin.getQueueManager().isPlayerInQueue(playerId)) {
            return;
        }
        
        // 获取VIP优先级
        int priority = plugin.getVipManager().getVIPPriority(player);
        
        // 加入队列
        plugin.getQueueManager().addToQueue(playerId, priority);
        
        plugin.getLogger().info("玩家 " + player.getName() + " 已加入队列 (优先级: " + priority + ")");
    }
    
    /**
     * 发送欢迎消息
     */
    private void sendWelcomeMessage(Player player) {
        player.sendMessage("§6§l===========================================");
        player.sendMessage("§a§l          欢迎来到 Minecraft 服务器");
        player.sendMessage("§7");
        player.sendMessage("§e您当前在排队服务器中，请耐心等待");
        player.sendMessage("§e当有空位时，系统会自动传送您到游戏服务器");
        player.sendMessage("§7");
        player.sendMessage("§b可用命令:");
        player.sendMessage("§f  /queue - 查看队列状态");
        player.sendMessage("§f  /leave - 离开队列");
        player.sendMessage("§6§l===========================================");
    }
    
    /**
     * 显示队列状态
     */
    private void showQueueStatus(Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    return;
                }
                
                try {
                    UUID playerId = player.getUniqueId();
                    int position = plugin.getQueueManager().getQueuePosition(playerId);
                    int totalInQueue = plugin.getQueueManager().getQueueSize();
                    
                    if (position > 0) {
                        player.sendMessage("§a§l队列状态:");
                        player.sendMessage("§f  您在队列中的位置: §e#" + position);
                        player.sendMessage("§f  队列总人数: §e" + totalInQueue);
                        
                        // 预估等待时间
                        int estimatedWaitTime = calculateEstimatedWaitTime(position);
                        if (estimatedWaitTime > 0) {
                            player.sendMessage("§f  预估等待时间: §e" + estimatedWaitTime + " 分钟");
                        }
                    } else {
                        player.sendMessage("§c您不在队列中！");
                    }
                    
                } catch (Exception e) {
                    plugin.getLogger().severe("显示队列状态失败: " + e.getMessage());
                }
            }
        }.runTaskAsynchronously(plugin);
    }
    
    /**
     * 计算预估等待时间
     */
    private int calculateEstimatedWaitTime(int position) {
        // 基于历史数据估算每分钟处理的玩家数量
        int playersPerMinute = 2; // 假设每分钟处理2个玩家
        return (position / playersPerMinute) + 1;
    }
    
    /**
     * 记录客户端信息（用于调试Forge兼容性问题）
     */
    private void logClientInfo(Player player) {
        try {
            if (!plugin.getConfigManager().isMohistClientConnectionLoggingEnabled()) {
                return;
            }
            
            String playerName = player.getName();
            String address = player.getAddress() != null ? player.getAddress().toString() : "unknown";
            
            // 记录基本连接信息
            plugin.getLogger().info("客户端连接信息:");
            plugin.getLogger().info("  玩家: " + playerName);
            plugin.getLogger().info("  地址: " + address);
            plugin.getLogger().info("  UUID: " + player.getUniqueId());
            
            // 尝试检测客户端类型
            String clientType = detectClientType(player);
            plugin.getLogger().info("  客户端类型: " + clientType);
            
            // 检查客户端兼容性
            if (plugin.getConfigManager().isMohistClientTypeDetectionEnabled()) {
                checkClientCompatibility(player, clientType);
            }
            
        } catch (Exception e) {
            plugin.getLogger().warning("记录客户端信息失败: " + e.getMessage());
        }
    }
    
    /**
     * 检测客户端类型
     */
    private String detectClientType(Player player) {
        try {
            // 检查是否有Forge相关的元数据
            if (player.hasMetadata("forge:handshake")) {
                return "Forge";
            } else if (player.hasMetadata("modloader")) {
                return "Modded";
            } else {
                // 通过其他方式检测
                // 注意：在代理环境下这些检测可能不准确
                return "Vanilla/Unknown";
            }
        } catch (Exception e) {
            plugin.getLogger().warning("客户端类型检测失败: " + e.getMessage());
            return "Unknown";
        }
    }
    
    /**
     * 检查客户端兼容性
     */
    private void checkClientCompatibility(Player player, String clientType) {
        try {
            boolean isVanilla = clientType.contains("Vanilla");
            boolean isForge = clientType.contains("Forge");
            
            // 检查是否允许该类型的客户端
            if (isVanilla && !plugin.getConfigManager().isVanillaClientsAllowed()) {
                handleIncompatibleClient(player, "原版客户端不被允许");
                return;
            }
            
            if (isForge && !plugin.getConfigManager().isForgeClientsAllowed()) {
                handleIncompatibleClient(player, "Forge客户端不被允许");
                return;
            }
            
            // 发送客户端类型检测消息
            String message = plugin.getConfigManager().getClientTypeDetectedMessage()
                .replace("{type}", clientType);
            player.sendMessage(message.replace("&", "§"));
            
            // 检查目标服务器兼容性
            if (isVanilla && plugin.getConfigManager().getTargetServer().contains("forge")) {
                String warningMessage = plugin.getConfigManager().getClientIncompatibleMessage();
                player.sendMessage(warningMessage.replace("&", "§"));
            }
            
        } catch (Exception e) {
            plugin.getLogger().warning("客户端兼容性检查失败: " + e.getMessage());
        }
    }
    
    /**
     * 处理不兼容的客户端
     */
    private void handleIncompatibleClient(Player player, String reason) {
        String action = plugin.getConfigManager().getTypeMismatchAction();
        
        switch (action.toLowerCase()) {
            case "kick":
                player.kickPlayer("§c客户端不兼容: " + reason);
                break;
            case "warn":
                player.sendMessage("§c警告: " + reason);
                plugin.getLogger().warning("玩家 " + player.getName() + " 客户端不兼容: " + reason);
                break;
            case "ignore":
                // 忽略，不采取任何行动
                break;
            default:
                plugin.getLogger().warning("未知的客户端不匹配行动: " + action);
                break;
        }
    }
    
    /**
     * 验证代理转发
     */
    private boolean validateProxyForwarding(Player player, PlayerLoginEvent event) {
        try {
            // 检查是否从代理服务器连接
            String playerAddress = player.getAddress().getAddress().getHostAddress();
            
            // 如果是本地连接但不是直接连接到队列服务器，可能是代理转发问题
            if ("127.0.0.1".equals(playerAddress) || "localhost".equals(playerAddress)) {
                plugin.getLogger().info("玩家 " + player.getName() + " 通过本地代理连接");
                return true;
            }
            
            // 检查是否有 BungeeCord/Velocity 转发头信息
            // 在 Spigot 环境下，如果启用了 bungeecord: true，
            // 那么非代理连接会被拒绝
            if (plugin.getConfigManager().getProxyType().equals("velocity")) {
                // Velocity 特定的验证逻辑
                plugin.getLogger().info("玩家 " + player.getName() + " 通过 Velocity 代理连接，IP: " + playerAddress);
            } else {
                // BungeeCord 特定的验证逻辑
                plugin.getLogger().info("玩家 " + player.getName() + " 通过 BungeeCord 代理连接，IP: " + playerAddress);
            }
            
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().severe("代理转发验证失败: " + e.getMessage());
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, 
                "§c§l连接验证失败\n\n§e请确保您通过正确的代理服务器连接\n§7错误: " + e.getMessage());
            return false;
        }
    }
}
