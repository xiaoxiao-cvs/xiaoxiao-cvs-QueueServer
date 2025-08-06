package com.github.queueserver.forge.http.model;

/**
 * 服务器就绪请求
 */
public class ServerReadyRequest {
    private String serverName;
    private boolean ready;
    private long timestamp;
    
    // Getters and Setters
    public String getServerName() {
        return serverName;
    }
    
    public void setServerName(String serverName) {
        this.serverName = serverName;
    }
    
    public boolean isReady() {
        return ready;
    }
    
    public void setReady(boolean ready) {
        this.ready = ready;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
