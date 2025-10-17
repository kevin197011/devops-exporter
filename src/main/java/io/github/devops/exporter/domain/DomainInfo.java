package io.github.devops.exporter.domain;

import java.time.LocalDateTime;

public class DomainInfo {
    private String domain;
    private LocalDateTime expirationDate;
    private long daysUntilExpiration;
    private boolean isExpired;
    private boolean isWarning;
    private String status;
    private String error;
    private LocalDateTime lastChecked;
    
    public DomainInfo(String domain) {
        this.domain = domain;
        this.lastChecked = LocalDateTime.now();
    }
    
    // Getters and Setters
    public String getDomain() {
        return domain;
    }
    
    public void setDomain(String domain) {
        this.domain = domain;
    }
    
    public LocalDateTime getExpirationDate() {
        return expirationDate;
    }
    
    public void setExpirationDate(LocalDateTime expirationDate) {
        this.expirationDate = expirationDate;
    }
    
    public long getDaysUntilExpiration() {
        return daysUntilExpiration;
    }
    
    public void setDaysUntilExpiration(long daysUntilExpiration) {
        this.daysUntilExpiration = daysUntilExpiration;
    }
    
    public boolean isExpired() {
        return isExpired;
    }
    
    public void setExpired(boolean expired) {
        isExpired = expired;
    }
    
    public boolean isWarning() {
        return isWarning;
    }
    
    public void setWarning(boolean warning) {
        isWarning = warning;
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
}