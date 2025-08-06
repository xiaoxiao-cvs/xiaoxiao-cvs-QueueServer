package com.github.queueserver.forge.vip;

import com.github.queueserver.forge.QueueForgePlugin;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * VIP管理器
 * 处理VIP权限检查和缓存
 */
public class VIPManager {
    
    private final QueueForgePlugin plugin;
    private final Logger logger;
    
    // VIP状态缓存
    private final Cache<UUID, Boolean> vipCache;
    
    // VIP权限节点
    private String vipPermission;
    private double vipPriorityMultiplier;
    
    public VIPManager(QueueForgePlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        
        // 初始化配置
        this.vipPermission = plugin.getConfigManager().getVipPermission();
        this.vipPriorityMultiplier = plugin.getConfigManager().getVipPriorityMultiplier();
        
        // 初始化VIP缓存
        this.vipCache = Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .removalListener((key, value, cause) -> {
                    if (cause.wasEvicted()) {
                        logger.fine("VIP缓存项已过期: " + key);
                    }
                })
                .build();
        
        logger.info("VIP管理器已初始化");
        logger.info("VIP权限节点: " + vipPermission);
        logger.info("VIP优先级倍数: " + vipPriorityMultiplier);
    }
    
    /**
     * 检查玩家是否为VIP
     */
    public boolean isVIP(Player player) {
        if (!plugin.getConfigManager().isVipEnabled()) {
            return false;
        }
        
        UUID playerId = player.getUniqueId();
        
        // 先从缓存获取
        Boolean cached = vipCache.getIfPresent(playerId);
        if (cached != null) {
            return cached;
        }
        
        // 检查权限
        boolean isVip = checkVipPermission(player);
        
        // 缓存结果
        vipCache.put(playerId, isVip);
        
        if (plugin.getConfigManager().isDebugMode()) {
            logger.info("玩家 " + player.getName() + " VIP状态: " + isVip);
        }
        
        return isVip;
    }
    
    /**
     * 检查VIP权限
     */
    private boolean checkVipPermission(Player player) {
        try {
            // 检查特定权限节点
            if (player.hasPermission(vipPermission)) {
                return true;
            }
            
            // 检查常见VIP权限
            if (player.hasPermission("queue.vip") || 
                player.hasPermission("queue.priority") ||
                player.hasPermission("vip.queue") ||
                player.hasPermission("priority.queue")) {
                return true;
            }
            
            // 检查OP权限
            if (player.isOp()) {
                return true;
            }
            
            // 检查组权限 (LuckPerms等权限插件)
            if (hasGroupPermission(player)) {
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            logger.warning("检查VIP权限时发生错误: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 检查组权限
     */
    private boolean hasGroupPermission(Player player) {
        try {
            // 常见VIP组名称
            String[] vipGroups = {"vip", "premium", "donator", "supporter", "mvp", "plus"};
            
            for (String group : vipGroups) {
                if (player.hasPermission("group." + group) ||
                    player.hasPermission("rank." + group) ||
                    player.hasPermission("luckperms.group." + group)) {
                    return true;
                }
            }
            
            return false;
            
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 强制刷新玩家VIP状态
     */
    public void refreshVipStatus(UUID playerId) {
        vipCache.invalidate(playerId);
        logger.info("已刷新玩家VIP状态: " + playerId);
    }
    
    /**
     * 强制刷新所有VIP状态
     */
    public void refreshAllVipStatus() {
        vipCache.invalidateAll();
        logger.info("已刷新所有玩家VIP状态");
    }
    
    /**
     * 获取VIP优先级倍数
     */
    public double getVipPriorityMultiplier() {
        return vipPriorityMultiplier;
    }
    
    /**
     * 计算VIP队列优先级
     */
    public int calculateVipPriority(Player player, int basePosition) {
        if (!isVIP(player)) {
            return basePosition;
        }
        
        // VIP玩家优先级更高，位置更靠前
        return Math.max(1, (int) (basePosition / vipPriorityMultiplier));
    }
    
    /**
     * 获取VIP缓存统计
     */
    public VipCacheStats getCacheStats() {
        return new VipCacheStats(
                (int) vipCache.estimatedSize(),
                vipCache.stats().hitRate(),
                vipCache.stats().missRate(),
                vipCache.stats().evictionCount()
        );
    }
    
    /**
     * 清理过期缓存
     */
    public void cleanup() {
        long sizeBefore = vipCache.estimatedSize();
        vipCache.cleanUp();
        long sizeAfter = vipCache.estimatedSize();
        
        if (sizeBefore != sizeAfter) {
            logger.info("VIP缓存清理完成: " + (sizeBefore - sizeAfter) + " 项被清理");
        }
    }
    
    /**
     * 关闭VIP管理器
     */
    public void shutdown() {
        vipCache.invalidateAll();
        logger.info("VIP管理器已关闭");
    }
    
    /**
     * 添加VIP到缓存（用于外部API）
     */
    public void setVipStatus(UUID playerId, boolean isVip) {
        vipCache.put(playerId, isVip);
        logger.info("设置玩家VIP状态: " + playerId + " -> " + isVip);
    }
    
    /**
     * VIP缓存统计信息
     */
    public static class VipCacheStats {
        private final int size;
        private final double hitRate;
        private final double missRate;
        private final long evictionCount;
        
        public VipCacheStats(int size, double hitRate, double missRate, long evictionCount) {
            this.size = size;
            this.hitRate = hitRate;
            this.missRate = missRate;
            this.evictionCount = evictionCount;
        }
        
        public int getSize() {
            return size;
        }
        
        public double getHitRate() {
            return hitRate;
        }
        
        public double getMissRate() {
            return missRate;
        }
        
        public long getEvictionCount() {
            return evictionCount;
        }
        
        @Override
        public String toString() {
            return String.format("VipCacheStats{size=%d, hitRate=%.2f%%, missRate=%.2f%%, evictions=%d}", 
                    size, hitRate * 100, missRate * 100, evictionCount);
        }
    }
}
