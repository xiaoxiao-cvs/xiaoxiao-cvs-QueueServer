package com.github.queueserver.forge.http.model;

/**
 * 玩家传送请求
 */
public class PlayerTransferRequest {
    private String playerId;
    private String playerName;
    private String sourceServer;
    private String targetServer;
    private long timestamp;
    
    // Getters and Setters
    public String getPlayerId() {
        return playerId;
    }
    
    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }
    
    public String getPlayerName() {
        return playerName;
    }
    
    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }
    
    public String getSourceServer() {
        return sourceServer;
    }
    
    public void setSourceServer(String sourceServer) {
        this.sourceServer = sourceServer;
    }
    
    public String getTargetServer() {
        return targetServer;
    }
    
    public void setTargetServer(String targetServer) {
        this.targetServer = targetServer;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
