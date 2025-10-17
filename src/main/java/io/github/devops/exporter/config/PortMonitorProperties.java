package io.github.devops.exporter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "port.monitor")
public class PortMonitorProperties {
    
    private boolean enabled = true;
    private int checkInterval = 300; // 秒
    private List<String> ports;
    private int connectionTimeout = 5000; // 毫秒
    
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
    
    public List<String> getPorts() {
        return ports;
    }
    
    public void setPorts(List<String> ports) {
        this.ports = ports;
    }
    
    public int getConnectionTimeout() {
        return connectionTimeout;
    }
    
    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }
}