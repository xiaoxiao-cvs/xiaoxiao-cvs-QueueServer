package com.github.queueserver.forge.http.model;

/**
 * 心跳请求
 */
public class HeartbeatRequest {
    private String serverName;
    private int onlinePlayers;
    private int maxPlayers;
    private double tps;
    private long timestamp;
    
    // Getters and Setters
    public String getServerName() {
        return serverName;
    }
    
    public void setServerName(String serverName) {
        this.serverName = serverName;
    }
    
    public int getOnlinePlayers() {
        return onlinePlayers;
    }
    
    public void setOnlinePlayers(int onlinePlayers) {
        this.onlinePlayers = onlinePlayers;
    }
    
    public int getMaxPlayers() {
        return maxPlayers;
    }
    
    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }
    
    public double getTps() {
        return tps;
    }
    
    public void setTps(double tps) {
        this.tps = tps;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
