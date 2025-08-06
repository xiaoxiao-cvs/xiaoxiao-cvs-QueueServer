package com.github.queueserver.forge.security;

import com.github.queueserver.forge.QueueForgePlugin;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerLoginEvent;

import java.net.InetAddress;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * 安全管理器
 * 处理安全检查和反作弊功能
 */
public class SecurityManager {
    
    private final QueueForgePlugin plugin;
    private final Logger logger;
    
    // IP白名单和黑名单
    private final Set<String> allowedIPs = ConcurrentHashMap.newKeySet();
    private final Set<String> blockedIPs = ConcurrentHashMap.newKeySet();
    
    // 反破解检查
    private final Set<String> suspiciousNames = ConcurrentHashMap.newKeySet();
    private final Pattern crackedNamePattern = Pattern.compile("^(Player|Notch|Steve|Alex|User|Test|Guest|Admin)\\d*$", Pattern.CASE_INSENSITIVE);
    
    // 连接频率限制
    private final ConcurrentHashMap<String, Long> connectionAttempts = new ConcurrentHashMap<>();
    private final long CONNECTION_COOLDOWN = 5000; // 5秒冷却时间
    
    public SecurityManager(QueueForgePlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        
        // 加载配置
        loadSecurityConfig();
        
        // 初始化一些常见的可疑用户名
        initializeSuspiciousNames();
        
        logger.info("安全管理器已初始化");
    }
    
    /**
     * 加载安全配置
     */
    private void loadSecurityConfig() {
        if (!plugin.getConfigManager().isSecurityEnabled()) {
            logger.info("安全检查已禁用");
            return;
        }
        
        // 加载IP白名单
        List<String> allowedIPsList = plugin.getConfigManager().getAllowedIps();
        allowedIPs.clear();
        allowedIPs.addAll(allowedIPsList);
        
        if (!allowedIPs.isEmpty()) {
            logger.info("已加载 " + allowedIPs.size() + " 个白名单IP");
        }
    }
    
    /**
     * 初始化可疑用户名列表
     */
    private void initializeSuspiciousNames() {
        suspiciousNames.add("Player");
        suspiciousNames.add("Notch");
        suspiciousNames.add("Steve");
        suspiciousNames.add("Alex");
        suspiciousNames.add("User");
        suspiciousNames.add("Test");
        suspiciousNames.add("Guest");
        suspiciousNames.add("Admin");
        suspiciousNames.add("Owner");
        suspiciousNames.add("Operator");
        suspiciousNames.add("Moderator");
        suspiciousNames.add("Helper");
    }
    
    /**
     * 检查玩家登录安全性
     */
    public SecurityCheckResult checkPlayerLogin(Player player, InetAddress address) {
        if (!plugin.getConfigManager().isSecurityEnabled()) {
            return SecurityCheckResult.ALLOWED;
        }
        
        String playerName = player.getName();
        String ipAddress = address.getHostAddress();
        
        // 检查IP白名单
        if (!allowedIPs.isEmpty() && !isIPAllowed(ipAddress)) {
            logger.warning("玩家 " + playerName + " 使用未授权IP: " + ipAddress);
            return new SecurityCheckResult(false, "IP地址未授权", SecurityCheckResult.RejectReason.IP_NOT_ALLOWED);
        }
        
        // 检查IP黑名单
        if (isIPBlocked(ipAddress)) {
            logger.warning("玩家 " + playerName + " 使用被封禁IP: " + ipAddress);
            return new SecurityCheckResult(false, "IP地址已被封禁", SecurityCheckResult.RejectReason.IP_BLOCKED);
        }
        
        // 检查连接频率
        if (isConnectionTooFrequent(ipAddress)) {
            logger.warning("玩家 " + playerName + " 连接过于频繁: " + ipAddress);
            return new SecurityCheckResult(false, "连接过于频繁，请稍后重试", SecurityCheckResult.RejectReason.TOO_FREQUENT);
        }
        
        // 反破解检查
        if (plugin.getConfigManager().isAntiCrackEnabled() && isSuspiciousPlayer(player)) {
            logger.warning("检测到可疑玩家: " + playerName + " IP: " + ipAddress);
            return new SecurityCheckResult(false, "检测到非正版客户端", SecurityCheckResult.RejectReason.CRACKED_CLIENT);
        }
        
        // 记录连接尝试
        recordConnectionAttempt(ipAddress);
        
        return SecurityCheckResult.ALLOWED;
    }
    
    /**
     * 检查IP是否在白名单中
     */
    private boolean isIPAllowed(String ipAddress) {
        if (allowedIPs.isEmpty()) {
            return true; // 如果白名单为空，允许所有IP
        }
        
        return allowedIPs.contains(ipAddress) || 
               allowedIPs.stream().anyMatch(allowed -> isIPInRange(ipAddress, allowed));
    }
    
    /**
     * 检查IP是否在黑名单中
     */
    private boolean isIPBlocked(String ipAddress) {
        return blockedIPs.contains(ipAddress);
    }
    
    /**
     * 检查IP是否在指定范围内
     */
    private boolean isIPInRange(String ipAddress, String range) {
        try {
            if (range.contains("/")) {
                // CIDR表示法
                return isIPInCIDR(ipAddress, range);
            } else if (range.contains("*")) {
                // 通配符表示法
                return ipAddress.matches(range.replace("*", ".*"));
            } else {
                // 精确匹配
                return ipAddress.equals(range);
            }
        } catch (Exception e) {
            logger.warning("IP范围检查失败: " + range + " - " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 检查IP是否在CIDR范围内
     */
    private boolean isIPInCIDR(String ipAddress, String cidr) {
        try {
            String[] parts = cidr.split("/");
            if (parts.length != 2) {
                return false;
            }
            
            InetAddress targetAddr = InetAddress.getByName(ipAddress);
            InetAddress rangeAddr = InetAddress.getByName(parts[0]);
            int prefixLength = Integer.parseInt(parts[1]);
            
            byte[] targetBytes = targetAddr.getAddress();
            byte[] rangeBytes = rangeAddr.getAddress();
            
            if (targetBytes.length != rangeBytes.length) {
                return false;
            }
            
            int bytesToCheck = prefixLength / 8;
            int bitsToCheck = prefixLength % 8;
            
            // 检查完整字节
            for (int i = 0; i < bytesToCheck; i++) {
                if (targetBytes[i] != rangeBytes[i]) {
                    return false;
                }
            }
            
            // 检查部分字节
            if (bitsToCheck > 0 && bytesToCheck < targetBytes.length) {
                int mask = 0xFF << (8 - bitsToCheck);
                return (targetBytes[bytesToCheck] & mask) == (rangeBytes[bytesToCheck] & mask);
            }
            
            return true;
            
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 检查连接是否过于频繁
     */
    private boolean isConnectionTooFrequent(String ipAddress) {
        Long lastConnection = connectionAttempts.get(ipAddress);
        if (lastConnection == null) {
            return false;
        }
        
        return System.currentTimeMillis() - lastConnection < CONNECTION_COOLDOWN;
    }
    
    /**
     * 记录连接尝试
     */
    private void recordConnectionAttempt(String ipAddress) {
        connectionAttempts.put(ipAddress, System.currentTimeMillis());
        
        // 清理过期记录
        long expireTime = System.currentTimeMillis() - CONNECTION_COOLDOWN * 2;
        connectionAttempts.entrySet().removeIf(entry -> entry.getValue() < expireTime);
    }
    
    /**
     * 检查是否为可疑玩家
     */
    private boolean isSuspiciousPlayer(Player player) {
        String playerName = player.getName();
        
        // 检查用户名模式
        if (crackedNamePattern.matcher(playerName).matches()) {
            return true;
        }
        
        // 检查可疑用户名列表
        if (suspiciousNames.stream().anyMatch(suspicious -> 
            playerName.toLowerCase().contains(suspicious.toLowerCase()))) {
            return true;
        }
        
        // 检查UUID模式（正版UUID有特定格式）
        String uuid = player.getUniqueId().toString();
        if (isOfflineUUID(uuid)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 检查是否为离线模式UUID
     */
    private boolean isOfflineUUID(String uuid) {
        // 离线模式UUID通常以特定模式生成
        // 这里做一个简单的检查
        return uuid.startsWith("00000000-0000-0000");
    }
    
    /**
     * 添加IP到黑名单
     */
    public void blockIP(String ipAddress) {
        blockedIPs.add(ipAddress);
        logger.info("IP已添加到黑名单: " + ipAddress);
    }
    
    /**
     * 从黑名单移除IP
     */
    public void unblockIP(String ipAddress) {
        blockedIPs.remove(ipAddress);
        logger.info("IP已从黑名单移除: " + ipAddress);
    }
    
    /**
     * 添加可疑用户名
     */
    public void addSuspiciousName(String name) {
        suspiciousNames.add(name);
        logger.info("用户名已添加到可疑列表: " + name);
    }
    
    /**
     * 获取安全统计信息
     */
    public SecurityStats getSecurityStats() {
        return new SecurityStats(
                allowedIPs.size(),
                blockedIPs.size(),
                suspiciousNames.size(),
                connectionAttempts.size()
        );
    }
    
    /**
     * 安全检查结果
     */
    public static class SecurityCheckResult {
        public static final SecurityCheckResult ALLOWED = new SecurityCheckResult(true, "允许", null);
        
        private final boolean allowed;
        private final String message;
        private final RejectReason reason;
        
        public SecurityCheckResult(boolean allowed, String message, RejectReason reason) {
            this.allowed = allowed;
            this.message = message;
            this.reason = reason;
        }
        
        public boolean isAllowed() {
            return allowed;
        }
        
        public String getMessage() {
            return message;
        }
        
        public RejectReason getReason() {
            return reason;
        }
        
        public enum RejectReason {
            IP_NOT_ALLOWED,
            IP_BLOCKED,
            TOO_FREQUENT,
            CRACKED_CLIENT,
            SUSPICIOUS_NAME
        }
    }
    
    /**
     * 安全统计信息
     */
    public static class SecurityStats {
        private final int allowedIPs;
        private final int blockedIPs;
        private final int suspiciousNames;
        private final int activeConnections;
        
        public SecurityStats(int allowedIPs, int blockedIPs, int suspiciousNames, int activeConnections) {
            this.allowedIPs = allowedIPs;
            this.blockedIPs = blockedIPs;
            this.suspiciousNames = suspiciousNames;
            this.activeConnections = activeConnections;
        }
        
        public int getAllowedIPs() {
            return allowedIPs;
        }
        
        public int getBlockedIPs() {
            return blockedIPs;
        }
        
        public int getSuspiciousNames() {
            return suspiciousNames;
        }
        
        public int getActiveConnections() {
            return activeConnections;
        }
        
        @Override
        public String toString() {
            return String.format("SecurityStats{allowedIPs=%d, blockedIPs=%d, suspiciousNames=%d, activeConnections=%d}", 
                    allowedIPs, blockedIPs, suspiciousNames, activeConnections);
        }
    }
}
