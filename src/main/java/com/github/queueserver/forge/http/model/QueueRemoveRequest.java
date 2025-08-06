package com.github.queueserver.forge.http.model;

/**
 * 队列移除请求
 */
public class QueueRemoveRequest {
    private String playerId;
    private long timestamp;
    
    // Getters and Setters
    public String getPlayerId() {
        return playerId;
    }
    
    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
