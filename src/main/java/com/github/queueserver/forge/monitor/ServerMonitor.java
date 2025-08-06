package com.github.queueserver.forge.monitor;

import com.github.queueserver.forge.QueueForgePlugin;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * 服务器监控器
 * 监控服务器性能和状态
 */
public class ServerMonitor {
    
    private final QueueForgePlugin plugin;
    private final Logger logger;
    private final ScheduledExecutorService scheduler;
    
    // 监控状态
    private volatile boolean monitoring = false;
    private volatile double currentTPS = 20.0;
    private volatile int currentPlayers = 0;
    private volatile long lastUpdate = 0;
    
    public ServerMonitor(QueueForgePlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.scheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "QueueForge-ServerMonitor");
            t.setDaemon(true);
            return t;
        });
    }
    
    /**
     * 开始监控
     */
    public void startMonitoring() {
        if (monitoring) {
            return;
        }
        
        monitoring = true;
        
        // TPS监控任务
        scheduler.scheduleAtFixedRate(this::updateTPS, 0, 1, TimeUnit.SECONDS);
        
        // 玩家数量监控任务
        scheduler.scheduleAtFixedRate(this::updatePlayerCount, 0, 5, TimeUnit.SECONDS);
        
        logger.info("服务器监控已启动");
    }
    
    /**
     * 停止监控
     */
    public void stopMonitoring() {
        monitoring = false;
        scheduler.shutdown();
        
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
        
        logger.info("服务器监控已停止");
    }
    
    /**
     * 更新TPS
     */
    private void updateTPS() {
        try {
            // 尝试获取Paper TPS
            double[] tpsArray = plugin.getServer().getTPS();
            if (tpsArray != null && tpsArray.length > 0) {
                currentTPS = Math.min(20.0, tpsArray[0]);
            } else {
                // 如果无法获取TPS，使用估算方法
                currentTPS = estimateTPS();
            }
            
            lastUpdate = System.currentTimeMillis();
            
        } catch (Exception e) {
            // 如果获取TPS失败，使用默认值
            currentTPS = 20.0;
        }
    }
    
    /**
     * 估算TPS
     */
    private double estimateTPS() {
        try {
            // 简单的TPS估算方法
            long start = System.nanoTime();
            
            // 执行一些轻量级操作
            int players = plugin.getServer().getOnlinePlayers().size();
            
            long end = System.nanoTime();
            long duration = end - start;
            
            // 基于响应时间估算TPS
            if (duration < 1_000_000) { // < 1ms
                return 20.0;
            } else if (duration < 10_000_000) { // < 10ms
                return 18.0;
            } else if (duration < 50_000_000) { // < 50ms
                return 15.0;
            } else {
                return 10.0;
            }
            
        } catch (Exception e) {
            return 20.0;
        }
    }
    
    /**
     * 更新玩家数量
     */
    private void updatePlayerCount() {
        try {
            currentPlayers = plugin.getServer().getOnlinePlayers().size();
        } catch (Exception e) {
            logger.warning("更新玩家数量失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取当前TPS
     */
    public double getCurrentTPS() {
        return currentTPS;
    }
    
    /**
     * 获取当前玩家数量
     */
    public int getCurrentPlayers() {
        return currentPlayers;
    }
    
    /**
     * 获取最大玩家数量
     */
    public int getMaxPlayers() {
        return plugin.getServer().getMaxPlayers();
    }
    
    /**
     * 检查服务器是否健康
     */
    public boolean isServerHealthy() {
        return currentTPS >= 18.0 && monitoring;
    }
    
    /**
     * 检查服务器是否过载
     */
    public boolean isServerOverloaded() {
        return currentTPS < 15.0;
    }
    
    /**
     * 获取服务器负载级别
     */
    public ServerLoadLevel getLoadLevel() {
        if (currentTPS >= 19.0) {
            return ServerLoadLevel.LOW;
        } else if (currentTPS >= 17.0) {
            return ServerLoadLevel.MEDIUM;
        } else if (currentTPS >= 15.0) {
            return ServerLoadLevel.HIGH;
        } else {
            return ServerLoadLevel.CRITICAL;
        }
    }
    
    /**
     * 获取服务器状态信息
     */
    public ServerStatus getServerStatus() {
        return new ServerStatus(
                monitoring,
                currentTPS,
                currentPlayers,
                getMaxPlayers(),
                getLoadLevel(),
                System.currentTimeMillis() - lastUpdate < 10000 // 最近10秒内有更新
        );
    }
    
    /**
     * 服务器负载级别
     */
    public enum ServerLoadLevel {
        LOW("低负载"),
        MEDIUM("中等负载"), 
        HIGH("高负载"),
        CRITICAL("严重负载");
        
        private final String description;
        
        ServerLoadLevel(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 服务器状态信息
     */
    public static class ServerStatus {
        private final boolean monitoring;
        private final double tps;
        private final int currentPlayers;
        private final int maxPlayers;
        private final ServerLoadLevel loadLevel;
        private final boolean dataFresh;
        
        public ServerStatus(boolean monitoring, double tps, int currentPlayers, int maxPlayers, 
                          ServerLoadLevel loadLevel, boolean dataFresh) {
            this.monitoring = monitoring;
            this.tps = tps;
            this.currentPlayers = currentPlayers;
            this.maxPlayers = maxPlayers;
            this.loadLevel = loadLevel;
            this.dataFresh = dataFresh;
        }
        
        public boolean isMonitoring() {
            return monitoring;
        }
        
        public double getTps() {
            return tps;
        }
        
        public int getCurrentPlayers() {
            return currentPlayers;
        }
        
        public int getMaxPlayers() {
            return maxPlayers;
        }
        
        public int getAvailableSlots() {
            return Math.max(0, maxPlayers - currentPlayers);
        }
        
        public ServerLoadLevel getLoadLevel() {
            return loadLevel;
        }
        
        public boolean isDataFresh() {
            return dataFresh;
        }
        
        public boolean isHealthy() {
            return tps >= 18.0 && monitoring && dataFresh;
        }
        
        @Override
        public String toString() {
            return String.format("ServerStatus{tps=%.1f, players=%d/%d, load=%s, healthy=%s}", 
                    tps, currentPlayers, maxPlayers, loadLevel.getDescription(), isHealthy());
        }
    }
}
