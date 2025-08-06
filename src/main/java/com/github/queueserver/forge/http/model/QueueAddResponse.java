package com.github.queueserver.forge.http.model;

/**
 * 队列添加响应
 */
public class QueueAddResponse {
    private boolean success;
    private String message;
    private int position;
    private long estimatedWaitTime;
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
    
    public int getPosition() {
        return position;
    }
    
    public void setPosition(int position) {
        this.position = position;
    }
    
    public long getEstimatedWaitTime() {
        return estimatedWaitTime;
    }
    
    public void setEstimatedWaitTime(long estimatedWaitTime) {
        this.estimatedWaitTime = estimatedWaitTime;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
