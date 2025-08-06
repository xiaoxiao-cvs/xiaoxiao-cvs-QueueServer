package com.github.queueserver.forge.http.model;

/**
 * 服务器状态响应
 */
public class ServerStatusResponse {
    private boolean online;
    private int currentPlayers;
    private int maxPlayers;
    private int availableSlots;
    private double tps;
    private String status;
    private long timestamp;
    
    // Getters and Setters
    public boolean isOnline() {
        return online;
    }
    
    public void setOnline(boolean online) {
        this.online = online;
    }
    
    public int getCurrentPlayers() {
        return currentPlayers;
    }
    
    public void setCurrentPlayers(int currentPlayers) {
        this.currentPlayers = currentPlayers;
    }
    
    public int getMaxPlayers() {
        return maxPlayers;
    }
    
    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }
    
    public int getAvailableSlots() {
        return availableSlots;
    }
    
    public void setAvailableSlots(int availableSlots) {
        this.availableSlots = availableSlots;
    }
    
    public double getTps() {
        return tps;
    }
    
    public void setTps(double tps) {
        this.tps = tps;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    /**
     * 检查是否有可用槽位
     */
    public boolean hasAvailableSlots() {
        return availableSlots > 0;
    }
}
