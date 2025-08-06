package com.github.queueserver.forge.listeners;

import com.github.queueserver.forge.QueueForgePlugin;
import com.github.queueserver.forge.security.SecurityManager;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerKickEvent;

import java.util.UUID;
import java.util.logging.Logger;

/**
 * 玩家连接事件监听器
 * 处理玩家加入、离开等事件
 */
public class PlayerConnectionListener implements Listener {
    
    private final QueueForgePlugin plugin;
    private final Logger logger;
    
    public PlayerConnectionListener(QueueForgePlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }
    
    /**
     * 玩家登录事件
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLogin(PlayerLoginEvent event) {
        if (!plugin.isServerReady()) {
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, 
                "§c服务器正在启动中...\n§e请稍后重试");
            return;
        }
        
        Player player = event.getPlayer();
        
        // 安全检查
        if (plugin.getConfigManager().isSecurityEnabled()) {
            SecurityManager.SecurityCheckResult securityResult = 
                plugin.getSecurityManager().checkPlayerLogin(player, event.getAddress());
            
            if (!securityResult.isAllowed()) {
                event.disallow(PlayerLoginEvent.Result.KICK_OTHER, 
                    "§c连接被拒绝\n§7原因: " + securityResult.getMessage());
                logger.warning("拒绝玩家 " + player.getName() + " 连接: " + securityResult.getMessage());
                return;
            }
        }
        
        // 检查队列模式
        if (plugin.getConfigManager().isQueueMode()) {
            // 检查是否已在队列中
            if (plugin.getQueueManager().isPlayerInQueue(player.getUniqueId())) {
                // 玩家已在队列中，允许连接但显示队列信息
                logger.info("队列中的玩家重新连接: " + player.getName());
            } else {
                // 新玩家，检查是否需要排队
                if (shouldPlayerQueue(player)) {
                    event.disallow(PlayerLoginEvent.Result.KICK_FULL, 
                        generateQueueMessage(player));
                    
                    // 添加到队列
                    addPlayerToQueue(player);
                    return;
                }
            }
        }
        
        logger.info("玩家 " + player.getName() + " 登录成功");
    }
    
    /**
     * 玩家加入事件
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        // 如果玩家在队列中，从队列移除
        if (plugin.getQueueManager().isPlayerInQueue(playerId)) {
            plugin.getQueueManager().removePlayer(playerId);
            plugin.getDatabaseManager().recordPlayerTransfer(playerId);
            
            // 发送欢迎消息
            player.sendMessage("§a§l欢迎！");
            player.sendMessage("§e您已成功通过队列进入服务器！");
            player.sendMessage("§7感谢您的耐心等待。");
        }
        
        // 更新VIP记录
        boolean isVip = plugin.getVipManager().isVIP(player);
        plugin.getDatabaseManager().updateVipRecord(playerId, player.getName(), isVip);
        
        logger.info("玩家 " + player.getName() + " 已加入服务器" + (isVip ? " (VIP)" : ""));
    }
    
    /**
     * 玩家离开事件
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        // 从队列中移除（如果存在）
        if (plugin.getQueueManager().isPlayerInQueue(playerId)) {
            plugin.getQueueManager().removePlayer(playerId);
            plugin.getDatabaseManager().recordPlayerLeaveQueue(playerId, "QUIT");
        }
        
        logger.info("玩家 " + player.getName() + " 已离开服务器");
    }
    
    /**
     * 玩家被踢出事件
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerKick(PlayerKickEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        // 从队列中移除（如果存在）
        if (plugin.getQueueManager().isPlayerInQueue(playerId)) {
            plugin.getQueueManager().removePlayer(playerId);
            plugin.getDatabaseManager().recordPlayerLeaveQueue(playerId, "KICKED");
        }
        
        logger.info("玩家 " + player.getName() + " 被踢出服务器: " + event.getReason());
    }
    
    /**
     * 检查玩家是否需要排队
     */
    private boolean shouldPlayerQueue(Player player) {
        // VIP玩家检查
        if (plugin.getVipManager().isVIP(player)) {
            // VIP玩家有更高的优先级，但仍可能需要排队
            int currentPlayers = plugin.getServer().getOnlinePlayers().size();
            int maxPlayers = plugin.getServer().getMaxPlayers();
            
            // 给VIP预留一些位置
            int vipReservedSlots = Math.max(1, maxPlayers / 10); // 预留10%的位置给VIP
            return currentPlayers >= (maxPlayers - vipReservedSlots);
        }
        
        // 普通玩家检查
        int currentPlayers = plugin.getServer().getOnlinePlayers().size();
        int maxPlayers = plugin.getServer().getMaxPlayers();
        
        return currentPlayers >= maxPlayers;
    }
    
    /**
     * 生成队列消息
     */
    private String generateQueueMessage(Player player) {
        boolean isVip = plugin.getVipManager().isVIP(player);
        
        String message = "§c§l服务器已满！\n\n";
        
        if (isVip) {
            message += "§6§lVIP玩家检测！\n";
            message += "§e您已被添加到VIP优先队列\n\n";
        } else {
            message += "§e您已被添加到队列\n\n";
        }
        
        message += "§7请保持客户端开启\n";
        message += "§7队列系统将自动为您分配位置\n\n";
        message += "§a§l重新连接以查看队列状态";
        
        return message;
    }
    
    /**
     * 添加玩家到队列
     */
    private void addPlayerToQueue(Player player) {
        try {
            boolean isVip = plugin.getVipManager().isVIP(player);
            UUID playerId = player.getUniqueId();
            String playerName = player.getName();
            
            // 添加到本地队列
            if (plugin.getQueueManager().addPlayer(player, isVip)) {
                // 记录到数据库
                plugin.getDatabaseManager().recordPlayerJoinQueue(playerId, playerName, isVip);
                
                logger.info("玩家 " + playerName + " 已添加到队列" + (isVip ? " (VIP)" : ""));
            } else {
                logger.warning("添加玩家到队列失败: " + playerName);
            }
            
        } catch (Exception e) {
            logger.severe("添加玩家到队列时发生错误: " + e.getMessage());
        }
    }
}
