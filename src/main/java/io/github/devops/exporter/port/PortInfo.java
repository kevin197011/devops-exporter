package io.github.devops.exporter.port;

import java.time.LocalDateTime;

public class PortInfo {
    private String target; // ip:port 格式
    private String host;
    private int port;
    private boolean isOpen;
    private String status;
    private String error;
    private LocalDateTime lastChecked;
    private long responseTimeMs;
    
    public PortInfo(String target) {
        this.target = target;
        this.lastChecked = LocalDateTime.now();
        parseTarget(target);
    }
    
    private void parseTarget(String target) {
        try {
            String[] parts = target.split(":");
            if (parts.length == 2) {
                this.host = parts[0].trim();
                this.port = Integer.parseInt(parts[1].trim());
            } else {
                throw new IllegalArgumentException("Invalid target format: " + target);
            }
        } catch (Exception e) {
            this.host = target;
            this.port = -1;
            this.status = "INVALID_FORMAT";
            this.error = "Invalid target format: " + e.getMessage();
        }
    }
    
    // Getters and Setters
    public String getTarget() {
        return target;
    }
    
    public void setTarget(String target) {
        this.target = target;
    }
    
    public String getHost() {
        return host;
    }
    
    public void setHost(String host) {
        this.host = host;
    }
    
    public int getPort() {
        return port;
    }
    
    public void setPort(int port) {
        this.port = port;
    }
    
    public boolean isOpen() {
        return isOpen;
    }
    
    public void setOpen(boolean open) {
        isOpen = open;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
    }
    
    public LocalDateTime getLastChecked() {
        return lastChecked;
    }
    
    public void setLastChecked(LocalDateTime lastChecked) {
        this.lastChecked = lastChecked;
    }
    
    public long getResponseTimeMs() {
        return responseTimeMs;
    }
    
    public void setResponseTimeMs(long responseTimeMs) {
        this.responseTimeMs = responseTimeMs;
    }
}