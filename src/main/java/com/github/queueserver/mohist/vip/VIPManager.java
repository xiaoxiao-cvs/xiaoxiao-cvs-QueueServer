package com.github.queueserver.mohist.vip;

import com.github.queueserver.mohist.QueueMohistPlugin;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mohist环境下的VIP管理器
 * 处理Forge Mod和Bukkit插件的权限兼容性
 */
public class VIPManager {
    
    private final QueueMohistPlugin plugin;
    private final Map<UUID, VIPData> vipPlayers = new ConcurrentHashMap<>();
    private final Map<String, Integer> permissionLevels = new ConcurrentHashMap<>();
    
    public VIPManager(QueueMohistPlugin plugin) {
        this.plugin = plugin;
        loadVIPConfig();
    }
    
    /**
     * 加载VIP配置
     */
    private void loadVIPConfig() {
        Map<String, Integer> priorityLevels = plugin.getConfigManager().getVipPriorityLevels();
        permissionLevels.clear();
        permissionLevels.putAll(priorityLevels);
        
        plugin.getLogger().info("VIP配置已加载，权限等级: " + permissionLevels);
    }
    
    /**
     * 检查玩家是否为VIP
     */
    public boolean isVIP(Player player) {
        if (!plugin.getConfigManager().isVipEnabled()) {
            return false;
        }
        
        // 检查缓存
        VIPData vipData = vipPlayers.get(player.getUniqueId());
        if (vipData != null && !vipData.isExpired()) {
            return vipData.getVipLevel() > 0;
        }
        
        // 检查权限
        int vipLevel = calculateVIPLevel(player);
        
        // 更新缓存
        vipPlayers.put(player.getUniqueId(), new VIPData(vipLevel));
        
        return vipLevel > 0;
    }
    
    /**
     * 获取玩家VIP优先级
     */
    public int getVIPPriority(Player player) {
        if (!isVIP(player)) {
            return 0;
        }
        
        VIPData vipData = vipPlayers.get(player.getUniqueId());
        return vipData != null ? vipData.getVipLevel() : 0;
    }
    
    /**
     * 计算玩家VIP等级
     * 支持Mohist环境下的多种权限系统
     */
    private int calculateVIPLevel(Player player) {
        int highestLevel = 0;
        
        // 检查Bukkit权限
        for (Map.Entry<String, Integer> entry : permissionLevels.entrySet()) {
            String permission = "queue.vip." + entry.getKey();
            if (player.hasPermission(permission)) {
                highestLevel = Math.max(highestLevel, entry.getValue());
            }
        }
        
        // 检查OP权限
        if (player.isOp() && highestLevel == 0) {
            highestLevel = getMaxVIPLevel();
        }
        
        // 如果有Vault权限插件，进行额外检查
        if (hasVaultSupport()) {
            int vaultLevel = checkVaultPermissions(player);
            highestLevel = Math.max(highestLevel, vaultLevel);
        }
        
        return highestLevel;
    }
    
    /**
     * 检查是否有Vault支持
     */
    private boolean hasVaultSupport() {
        try {
            Class.forName("net.milkbowl.vault.permission.Permission");
            return plugin.getServer().getPluginManager().getPlugin("Vault") != null;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    /**
     * 通过Vault检查权限
     */
    private int checkVaultPermissions(Player player) {
        // 这里可以实现Vault权限检查逻辑
        // 由于需要Vault API，这里只是占位符
        return 0;
    }
    
    /**
     * 获取最高VIP等级
     */
    private int getMaxVIPLevel() {
        return permissionLevels.values().stream()
            .mapToInt(Integer::intValue)
            .max()
            .orElse(1);
    }
    
    /**
     * 手动设置玩家VIP状态
     */
    public void setVIPStatus(UUID playerId, int vipLevel) {
        if (vipLevel > 0) {
            vipPlayers.put(playerId, new VIPData(vipLevel));
            plugin.getLogger().info("玩家 " + playerId + " VIP等级已设置为: " + vipLevel);
        } else {
            vipPlayers.remove(playerId);
            plugin.getLogger().info("玩家 " + playerId + " VIP状态已移除");
        }
        
        // 保存到数据库
        try {
            plugin.getDatabaseManager().updateVIPStatus(playerId, vipLevel);
        } catch (Exception e) {
            plugin.getLogger().severe("保存VIP状态到数据库失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取VIP玩家数量
     */
    public int getVIPPlayerCount() {
        return (int) vipPlayers.values().stream()
            .filter(vipData -> !vipData.isExpired() && vipData.getVipLevel() > 0)
            .count();
    }
    
    /**
     * 清理过期的VIP缓存
     */
    public void cleanupExpiredCache() {
        int beforeSize = vipPlayers.size();
        vipPlayers.entrySet().removeIf(entry -> entry.getValue().isExpired());
        int afterSize = vipPlayers.size();
        
        if (beforeSize != afterSize) {
            plugin.getLogger().info("清理了 " + (beforeSize - afterSize) + " 个过期的VIP缓存");
        }
    }
    
    /**
     * 重载VIP配置
     */
    public void reloadConfig() {
        loadVIPConfig();
        vipPlayers.clear(); // 清理缓存，强制重新检查
        plugin.getLogger().info("VIP管理器配置已重载");
    }
    
    /**
     * 获取玩家VIP信息
     */
    public VIPInfo getVIPInfo(Player player) {
        boolean isVip = isVIP(player);
        int level = isVip ? getVIPPriority(player) : 0;
        String levelName = getLevelName(level);
        
        return new VIPInfo(isVip, level, levelName);
    }
    
    /**
     * 根据等级获取等级名称
     */
    private String getLevelName(int level) {
        switch (level) {
            case 1: return "青铜VIP";
            case 2: return "白银VIP";
            case 3: return "黄金VIP";
            case 4: return "钻石VIP";
            case 5: return "至尊VIP";
            default: return level > 0 ? "VIP" + level : "普通玩家";
        }
    }
    
    /**
     * VIP数据类
     */
    private static class VIPData {
        private final int vipLevel;
        private final long cacheTime;
        private static final long CACHE_DURATION = 5 * 60 * 1000; // 5分钟缓存
        
        public VIPData(int vipLevel) {
            this.vipLevel = vipLevel;
            this.cacheTime = System.currentTimeMillis();
        }
        
        public int getVipLevel() {
            return vipLevel;
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() - cacheTime > CACHE_DURATION;
        }
    }
    
    /**
     * VIP信息类
     */
    public static class VIPInfo {
        private final boolean isVip;
        private final int level;
        private final String levelName;
        
        public VIPInfo(boolean isVip, int level, String levelName) {
            this.isVip = isVip;
            this.level = level;
            this.levelName = levelName;
        }
        
        public boolean isVip() { return isVip; }
        public int getLevel() { return level; }
        public String getLevelName() { return levelName; }
    }
}
