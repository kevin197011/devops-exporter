package io.github.devops.exporter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "http.monitor")
public class HttpMonitorProperties {
    
    private boolean enabled = true;
    private int checkInterval = 300; // 秒
    private List<String> urls;
    private int connectionTimeout = 10000; // 毫秒
    private int readTimeout = 15000; // 毫秒
    private List<Integer> expectedStatusCodes = List.of(200, 201, 202, 204);
    private boolean followRedirects = true;
    
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
    
    public List<String> getUrls() {
        return urls;
    }
    
    public void setUrls(List<String> urls) {
        this.urls = urls;
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
    
    public List<Integer> getExpectedStatusCodes() {
        return expectedStatusCodes;
    }
    
    public void setExpectedStatusCodes(List<Integer> expectedStatusCodes) {
        this.expectedStatusCodes = expectedStatusCodes;
    }
    
    public boolean isFollowRedirects() {
        return followRedirects;
    }
    
    public void setFollowRedirects(boolean followRedirects) {
        this.followRedirects = followRedirects;
    }
}