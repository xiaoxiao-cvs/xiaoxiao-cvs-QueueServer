package com.github.queueserver.forge.queue;

import com.github.queueserver.forge.QueueForgePlugin;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

/**
 * 队列管理器
 * 处理本地队列逻辑和HTTP同步
 */
public class QueueManager {
    
    private final QueueForgePlugin plugin;
    private final Logger logger;
    
    // 队列存储
    private final Queue<QueueEntry> vipQueue = new ConcurrentLinkedQueue<>();
    private final Queue<QueueEntry> regularQueue = new ConcurrentLinkedQueue<>();
    
    // 队列缓存
    private final Cache<UUID, QueueEntry> queueCache;
    
    // 读写锁
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    
    // 统计信息
    private long lastProcessTime = 0;
    private int processedToday = 0;
    
    public QueueManager(QueueForgePlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        
        // 初始化缓存
        this.queueCache = Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .removalListener((key, value, cause) -> {
                    if (cause.wasEvicted()) {
                        logger.info("队列缓存项已过期: " + key);
                    }
                })
                .build();
        
        logger.info("队列管理器已初始化");
    }
    
    /**
     * 添加玩家到队列
     */
    public boolean addPlayer(Player player, boolean isVip) {
        UUID playerId = player.getUniqueId();
        
        lock.writeLock().lock();
        try {
            // 检查玩家是否已在队列中
            if (isPlayerInQueue(playerId)) {
                return false;
            }
            
            // 创建队列条目
            QueueEntry entry = new QueueEntry(playerId, player.getName(), isVip);
            
            // 添加到适当的队列
            if (isVip) {
                vipQueue.offer(entry);
                logger.info("VIP玩家 " + player.getName() + " 已加入队列");
            } else {
                regularQueue.offer(entry);
                logger.info("玩家 " + player.getName() + " 已加入队列");
            }
            
            // 添加到缓存
            queueCache.put(playerId, entry);
            
            // 通知代理服务器
            plugin.getProxyHttpClient().addPlayerToQueue(playerId, player.getName(), isVip);
            
            return true;
            
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * 从队列移除玩家
     */
    public boolean removePlayer(UUID playerId) {
        lock.writeLock().lock();
        try {
            QueueEntry entry = queueCache.getIfPresent(playerId);
            if (entry == null) {
                return false;
            }
            
            // 从队列中移除
            boolean removed = false;
            if (entry.isVip()) {
                removed = vipQueue.removeIf(e -> e.getPlayerId().equals(playerId));
            } else {
                removed = regularQueue.removeIf(e -> e.getPlayerId().equals(playerId));
            }
            
            if (removed) {
                queueCache.invalidate(playerId);
                logger.info("玩家 " + entry.getPlayerName() + " 已从队列移除");
                
                // 通知代理服务器
                plugin.getProxyHttpClient().removePlayerFromQueue(playerId);
            }
            
            return removed;
            
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * 获取下一个要处理的玩家
     */
    public UUID getNextPlayer() {
        lock.writeLock().lock();
        try {
            // 优先处理VIP队列
            QueueEntry entry = vipQueue.poll();
            if (entry == null) {
                entry = regularQueue.poll();
            }
            
            if (entry != null) {
                queueCache.invalidate(entry.getPlayerId());
                lastProcessTime = System.currentTimeMillis();
                processedToday++;
                
                logger.info("处理队列玩家: " + entry.getPlayerName());
                return entry.getPlayerId();
            }
            
            return null;
            
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * 检查玩家是否在队列中
     */
    public boolean isPlayerInQueue(UUID playerId) {
        return queueCache.getIfPresent(playerId) != null;
    }
    
    /**
     * 获取玩家在队列中的位置
     */
    public int getPlayerPosition(UUID playerId) {
        lock.readLock().lock();
        try {
            QueueEntry playerEntry = queueCache.getIfPresent(playerId);
            if (playerEntry == null) {
                return -1;
            }
            
            int position = 1;
            
            // 如果是VIP，只计算VIP队列中的位置
            if (playerEntry.isVip()) {
                for (QueueEntry entry : vipQueue) {
                    if (entry.getPlayerId().equals(playerId)) {
                        return position;
                    }
                    position++;
                }
            } else {
                // 如果是普通玩家，先计算所有VIP，再计算普通队列中的位置
                position += vipQueue.size();
                
                for (QueueEntry entry : regularQueue) {
                    if (entry.getPlayerId().equals(playerId)) {
                        return position;
                    }
                    position++;
                }
            }
            
            return -1;
            
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * 获取队列总大小
     */
    public int getTotalQueueSize() {
        lock.readLock().lock();
        try {
            return vipQueue.size() + regularQueue.size();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * 获取VIP队列大小
     */
    public int getVipQueueSize() {
        lock.readLock().lock();
        try {
            return vipQueue.size();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * 获取普通队列大小
     */
    public int getRegularQueueSize() {
        lock.readLock().lock();
        try {
            return regularQueue.size();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * 检查队列是否有玩家
     */
    public boolean hasPlayersInQueue() {
        return getTotalQueueSize() > 0;
    }
    
    /**
     * 移除离线玩家
     */
    public void removeOfflinePlayers() {
        lock.writeLock().lock();
        try {
            Set<UUID> toRemove = new HashSet<>();
            
            // 检查VIP队列
            vipQueue.removeIf(entry -> {
                Player player = plugin.getServer().getPlayer(entry.getPlayerId());
                if (player == null || !player.isOnline()) {
                    toRemove.add(entry.getPlayerId());
                    return true;
                }
                return false;
            });
            
            // 检查普通队列
            regularQueue.removeIf(entry -> {
                Player player = plugin.getServer().getPlayer(entry.getPlayerId());
                if (player == null || !player.isOnline()) {
                    toRemove.add(entry.getPlayerId());
                    return true;
                }
                return false;
            });
            
            // 清理缓存
            toRemove.forEach(queueCache::invalidate);
            
            if (!toRemove.isEmpty()) {
                logger.info("已清理 " + toRemove.size() + " 个离线玩家");
            }
            
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * 获取队列统计信息
     */
    public QueueStats getQueueStats() {
        lock.readLock().lock();
        try {
            return new QueueStats(
                getTotalQueueSize(),
                getVipQueueSize(),
                getRegularQueueSize(),
                lastProcessTime,
                processedToday
            );
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * 清空所有队列
     */
    public void clearAllQueues() {
        lock.writeLock().lock();
        try {
            vipQueue.clear();
            regularQueue.clear();
            queueCache.invalidateAll();
            logger.info("所有队列已清空");
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * 关闭队列管理器
     */
    public void shutdown() {
        clearAllQueues();
        logger.info("队列管理器已关闭");
    }
    
    /**
     * 队列条目类
     */
    public static class QueueEntry {
        private final UUID playerId;
        private final String playerName;
        private final boolean vip;
        private final long joinTime;
        
        public QueueEntry(UUID playerId, String playerName, boolean vip) {
            this.playerId = playerId;
            this.playerName = playerName;
            this.vip = vip;
            this.joinTime = System.currentTimeMillis();
        }
        
        public UUID getPlayerId() {
            return playerId;
        }
        
        public String getPlayerName() {
            return playerName;
        }
        
        public boolean isVip() {
            return vip;
        }
        
        public long getJoinTime() {
            return joinTime;
        }
        
        public long getWaitTime() {
            return System.currentTimeMillis() - joinTime;
        }
    }
    
    /**
     * 队列统计信息类
     */
    public static class QueueStats {
        private final int totalSize;
        private final int vipSize;
        private final int regularSize;
        private final long lastProcessTime;
        private final int processedToday;
        
        public QueueStats(int totalSize, int vipSize, int regularSize, long lastProcessTime, int processedToday) {
            this.totalSize = totalSize;
            this.vipSize = vipSize;
            this.regularSize = regularSize;
            this.lastProcessTime = lastProcessTime;
            this.processedToday = processedToday;
        }
        
        public int getTotalSize() {
            return totalSize;
        }
        
        public int getVipSize() {
            return vipSize;
        }
        
        public int getRegularSize() {
            return regularSize;
        }
        
        public long getLastProcessTime() {
            return lastProcessTime;
        }
        
        public int getProcessedToday() {
            return processedToday;
        }
    }
}
