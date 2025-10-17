package io.github.devops.exporter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "domain.monitor")
public class DomainMonitorProperties {
    
    private boolean enabled = true;
    private int checkInterval = 3600; // 秒
    private int warningDays = 30;
    private List<String> domains;
    private int connectionTimeout = 5000; // 毫秒
    private int readTimeout = 10000; // 毫秒
    
    // Getters and Setters
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public int getCheckInterval() {
        return checkInterval;
    }
    
    public void setCheckInterval(int checkInterval) {
        this.checkInterval = checkInterval;
    }
    
    public int getWarningDays() {
        return warningDays;
    }
    
    public void setWarningDays(int warningDays) {
        this.warningDays = warningDays;
    }
    
    public List<String> getDomains() {
        return domains;
    }
    
    public void setDomains(List<String> domains) {
        this.domains = domains;
    }
    
    public int getConnectionTimeout() {
        return connectionTimeout;
    }
    
    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }
    
    public int getReadTimeout() {
        return readTimeout;
    }
    
    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }
}