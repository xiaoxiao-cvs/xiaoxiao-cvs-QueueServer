package com.github.queueserver.mohist.compatibility;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Twilight Forest模组崩溃处理器
 * 专门处理暮色森林模组导致的客户端崩溃问题
 */
public class TwilightForestCrashHandler implements Listener {
    
    private final JavaPlugin plugin;
    private final Map<UUID, Long> playerConnectionTimes = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> playerCrashCount = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastCrashTime = new ConcurrentHashMap<>();
    
    // 崩溃检测配置
    private static final long FAST_DISCONNECT_THRESHOLD = 10000; // 10秒内断连视为可能崩溃
    private static final int MAX_CRASH_COUNT = 3; // 最大崩溃次数
    private static final long CRASH_RESET_TIME = 300000; // 5分钟后重置崩溃计数
    
    public TwilightForestCrashHandler(JavaPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * 处理玩家加入事件
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        
        // 记录连接时间
        playerConnectionTimes.put(playerId, currentTime);
        
        // 检查是否是崩溃后重连
        if (lastCrashTime.containsKey(playerId)) {
            long timeSinceLastCrash = currentTime - lastCrashTime.get(playerId);
            
            if (timeSinceLastCrash < CRASH_RESET_TIME) {
                int crashCount = playerCrashCount.getOrDefault(playerId, 0);
                
                if (crashCount > 0) {
                    plugin.getLogger().info("玩家 " + player.getName() + " 疑似从Twilight Forest崩溃中恢复 (崩溃次数: " + crashCount + ")");
                    
                    // 发送恢复消息
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (player.isOnline()) {
                            player.sendMessage("§c§l[崩溃恢复] §7检测到您可能遇到了客户端崩溃");
                            player.sendMessage("§e§l[建议] §7如果继续崩溃，请尝试：");
                            player.sendMessage("§7  1. 重启Minecraft客户端");
                            player.sendMessage("§7  2. 检查Twilight Forest模组版本");
                            player.sendMessage("§7  3. 减少视距设置");
                            
                            if (crashCount >= 2) {
                                player.sendMessage("§c§l[警告] §7多次崩溃检测，建议联系管理员");
                            }
                        }
                    }, 40L); // 2秒延迟
                }
            } else {
                // 重置崩溃计数
                playerCrashCount.remove(playerId);
                lastCrashTime.remove(playerId);
            }
        }
    }
    
    /**
     * 处理玩家退出事件
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        
        // 获取连接时间
        Long connectionTime = playerConnectionTimes.get(playerId);
        if (connectionTime != null) {
            long sessionDuration = currentTime - connectionTime;
            
            // 检查是否为快速断连（可能是崩溃）
            if (sessionDuration < FAST_DISCONNECT_THRESHOLD) {
                handlePossibleCrash(player, sessionDuration);
            }
            
            // 清理连接时间记录
            playerConnectionTimes.remove(playerId);
        }
    }
    
    /**
     * 处理可能的崩溃
     */
    private void handlePossibleCrash(Player player, long sessionDuration) {
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        
        // 增加崩溃计数
        int crashCount = playerCrashCount.getOrDefault(playerId, 0) + 1;
        playerCrashCount.put(playerId, crashCount);
        lastCrashTime.put(playerId, currentTime);
        
        // 记录崩溃信息
        plugin.getLogger().warning("检测到玩家 " + player.getName() + " 可能遇到客户端崩溃:");
        plugin.getLogger().warning("  会话时长: " + sessionDuration + "ms");
        plugin.getLogger().warning("  崩溃次数: " + crashCount);
        plugin.getLogger().warning("  断连类型: 快速断连（疑似崩溃）");
        
        // 检查是否是Twilight Forest相关崩溃（基于会话时长判断）
        if (isTwilightForestCrash(sessionDuration)) {
            plugin.getLogger().warning("  崩溃类型: 疑似Twilight Forest模组相关");
            
            // 如果崩溃次数过多，采取保护措施
            if (crashCount >= MAX_CRASH_COUNT) {
                plugin.getLogger().severe("玩家 " + player.getName() + " 崩溃次数过多，建议管理员介入处理");
                
                // 通知在线管理员
                notifyAdmins(player, crashCount);
            }
        }
    }
    
    /**
     * 检查是否是Twilight Forest相关崩溃
     */
    private boolean isTwilightForestCrash(long sessionDuration) {
        // 如果会话时长很短（小于5秒），很可能是模组注册表错误导致的崩溃
        return sessionDuration < 5000;
    }
    
    /**
     * 通知在线管理员
     */
    private void notifyAdmins(Player crashedPlayer, int crashCount) {
        String message = "§c§l[崩溃警告] §f玩家 " + crashedPlayer.getName() + 
                        " 连续崩溃 " + crashCount + " 次，可能是Twilight Forest模组问题";
        
        for (Player admin : Bukkit.getOnlinePlayers()) {
            if (admin.hasPermission("queueserver.admin.notify")) {
                admin.sendMessage(message);
            }
        }
        
        // 同时记录到控制台
        plugin.getLogger().severe(message.replaceAll("§[0-9a-fk-or]", ""));
    }
    
    /**
     * 获取玩家崩溃统计
     */
    public String getCrashStats(UUID playerId) {
        int crashCount = playerCrashCount.getOrDefault(playerId, 0);
        Long lastCrash = lastCrashTime.get(playerId);
        
        if (crashCount == 0) {
            return "该玩家无崩溃记录";
        }
        
        StringBuilder stats = new StringBuilder();
        stats.append("崩溃次数: ").append(crashCount);
        
        if (lastCrash != null) {
            long timeSince = System.currentTimeMillis() - lastCrash;
            stats.append(", 最后崩溃: ").append(timeSince / 1000).append("秒前");
        }
        
        return stats.toString();
    }
    
    /**
     * 清理过期的崩溃记录
     */
    public void cleanupExpiredRecords() {
        long currentTime = System.currentTimeMillis();
        
        lastCrashTime.entrySet().removeIf(entry -> {
            long timeSince = currentTime - entry.getValue();
            if (timeSince > CRASH_RESET_TIME) {
                playerCrashCount.remove(entry.getKey());
                return true;
            }
            return false;
        });
    }
}
