package com.github.queueserver.mohist.queue;

import com.github.queueserver.mohist.QueueMohistPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

/**
 * 简化的队列管理器
 */
public class SimpleQueueManager {
    
    private final QueueMohistPlugin plugin;
    private final Logger logger;
    
    // 普通队列和VIP队列
    private final Queue<UUID> normalQueue = new ConcurrentLinkedQueue<>();
    private final Queue<UUID> vipQueue = new ConcurrentLinkedQueue<>();
    
    // 玩家优先级映射
    private final Map<UUID, Integer> playerPriorities = new ConcurrentHashMap<>();
    
    public SimpleQueueManager(QueueMohistPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }
    
    /**
     * 添加玩家到队列
     */
    public void addToQueue(UUID playerId, int priority) {
        // 移除玩家（如果已在队列中）
        removeFromQueue(playerId);
        
        // 存储优先级
        playerPriorities.put(playerId, priority);
        
        // 根据优先级添加到不同队列
        if (priority > 0) {
            vipQueue.offer(playerId);
            logger.info("VIP玩家 " + playerId + " 加入队列 (优先级: " + priority + ")");
        } else {
            normalQueue.offer(playerId);
            logger.info("普通玩家 " + playerId + " 加入队列");
        }
    }
    
    /**
     * 从队列中移除玩家
     */
    public void removeFromQueue(UUID playerId) {
        boolean removed = vipQueue.remove(playerId) || normalQueue.remove(playerId);
        if (removed) {
            playerPriorities.remove(playerId);
            logger.info("玩家 " + playerId + " 已从队列中移除");
        }
    }
    
    /**
     * 获取下一个要传送的玩家
     */
    public UUID getNextPlayerInQueue() {
        // 优先从VIP队列获取
        UUID nextPlayer = vipQueue.poll();
        if (nextPlayer != null) {
            playerPriorities.remove(nextPlayer);
            return nextPlayer;
        }
        
        // 然后从普通队列获取
        nextPlayer = normalQueue.poll();
        if (nextPlayer != null) {
            playerPriorities.remove(nextPlayer);
            return nextPlayer;
        }
        
        return null;
    }
    
    /**
     * 检查玩家是否在队列中
     */
    public boolean isPlayerInQueue(UUID playerId) {
        return vipQueue.contains(playerId) || normalQueue.contains(playerId);
    }
    
    /**
     * 获取玩家在队列中的位置
     */
    public int getQueuePosition(UUID playerId) {
        // 检查VIP队列
        int position = 1;
        for (UUID uuid : vipQueue) {
            if (uuid.equals(playerId)) {
                return position;
            }
            position++;
        }
        
        // 检查普通队列
        for (UUID uuid : normalQueue) {
            if (uuid.equals(playerId)) {
                return position;
            }
            position++;
        }
        
        return -1; // 不在队列中
    }
    
    /**
     * 获取队列总大小
     */
    public int getQueueSize() {
        return vipQueue.size() + normalQueue.size();
    }
    
    /**
     * 获取VIP队列大小
     */
    public int getVipQueueSize() {
        return vipQueue.size();
    }
    
    /**
     * 获取普通队列大小
     */
    public int getNormalQueueSize() {
        return normalQueue.size();
    }
    
    /**
     * 获取所有排队的玩家
     */
    public Set<UUID> getAllQueuedPlayers() {
        Set<UUID> allPlayers = new HashSet<>();
        allPlayers.addAll(vipQueue);
        allPlayers.addAll(normalQueue);
        return allPlayers;
    }
    
    /**
     * 清空所有队列
     */
    public void clearAllQueues() {
        int totalRemoved = vipQueue.size() + normalQueue.size();
        vipQueue.clear();
        normalQueue.clear();
        playerPriorities.clear();
        
        logger.info("已清空所有队列，共移除 " + totalRemoved + " 名玩家");
    }
    
    /**
     * 获取队列统计信息
     */
    public QueueStats getQueueStats() {
        return new QueueStats(
            getQueueSize(),
            getVipQueueSize(),
            getNormalQueueSize()
        );
    }
    
    /**
     * 队列统计信息类
     */
    public static class QueueStats {
        private final int totalSize;
        private final int vipSize;
        private final int normalSize;
        
        public QueueStats(int totalSize, int vipSize, int normalSize) {
            this.totalSize = totalSize;
            this.vipSize = vipSize;
            this.normalSize = normalSize;
        }
        
        public int getTotalSize() { return totalSize; }
        public int getVipSize() { return vipSize; }
        public int getNormalSize() { return normalSize; }
    }
}
