package com.github.queueserver.forge.http.model;

/**
 * 玩家传送响应
 */
public class PlayerTransferResponse {
    private boolean success;
    private String message;
    private String transferId;
    private long timestamp;
    
    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getTransferId() {
        return transferId;
    }
    
    public void setTransferId(String transferId) {
        this.transferId = transferId;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
