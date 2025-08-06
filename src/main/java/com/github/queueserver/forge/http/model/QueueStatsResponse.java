package com.github.queueserver.forge.http.model;

/**
 * 队列统计响应
 */
public class QueueStatsResponse {
    private int totalPlayers;
    private int vipPlayers;
    private int regularPlayers;
    private double averageWaitTime;
    private long lastProcessTime;
    private boolean processing;
    private long timestamp;
    
    // Getters and Setters
    public int getTotalPlayers() {
        return totalPlayers;
    }
    
    public void setTotalPlayers(int totalPlayers) {
        this.totalPlayers = totalPlayers;
    }
    
    public int getVipPlayers() {
        return vipPlayers;
    }
    
    public void setVipPlayers(int vipPlayers) {
        this.vipPlayers = vipPlayers;
    }
    
    public int getRegularPlayers() {
        return regularPlayers;
    }
    
    public void setRegularPlayers(int regularPlayers) {
        this.regularPlayers = regularPlayers;
    }
    
    public double getAverageWaitTime() {
        return averageWaitTime;
    }
    
    public void setAverageWaitTime(double averageWaitTime) {
        this.averageWaitTime = averageWaitTime;
    }
    
    public long getLastProcessTime() {
        return lastProcessTime;
    }
    
    public void setLastProcessTime(long lastProcessTime) {
        this.lastProcessTime = lastProcessTime;
    }
    
    public boolean isProcessing() {
        return processing;
    }
    
    public void setProcessing(boolean processing) {
        this.processing = processing;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
