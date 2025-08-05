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
}
