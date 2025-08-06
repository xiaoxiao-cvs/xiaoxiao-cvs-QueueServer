package com.github.queueserver.forge.http.model;

/**
 * 队列信息响应
 */
public class QueueInfoResponse {
    private boolean inQueue;
    private int position;
    private int totalSize;
    private long estimatedWaitTime;
    private boolean vip;
    private String message;
    private long timestamp;
    
    // Getters and Setters
    public boolean isInQueue() {
        return inQueue;
    }
    
    public void setInQueue(boolean inQueue) {
        this.inQueue = inQueue;
    }
    
    public int getPosition() {
        return position;
    }
    
    public void setPosition(int position) {
        this.position = position;
    }
    
    public int getTotalSize() {
        return totalSize;
    }
    
    public void setTotalSize(int totalSize) {
        this.totalSize = totalSize;
    }
    
    public long getEstimatedWaitTime() {
        return estimatedWaitTime;
    }
    
    public void setEstimatedWaitTime(long estimatedWaitTime) {
        this.estimatedWaitTime = estimatedWaitTime;
    }
    
    public boolean isVip() {
        return vip;
    }
    
    public void setVip(boolean vip) {
        this.vip = vip;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
