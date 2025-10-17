package io.github.devops.exporter.http;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class HttpMetricsService {
    
    private final MeterRegistry meterRegistry;
    private final Map<String, HttpInfo> httpInfoCache = new ConcurrentHashMap<>();
    private final Set<String> registeredMetrics = ConcurrentHashMap.newKeySet();
    
    public HttpMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }
    
    public void updateHttpMetrics(HttpInfo httpInfo) {
        String url = httpInfo.getUrl();
        httpInfoCache.put(url, httpInfo);
        
        // 注册指标（只注册一次）
        registerHttpMetrics(url);
    }
    
    private void registerHttpMetrics(String url) {
        String metricKey = "http_metrics_" + url.hashCode();
        
        // 避免重复注册
        if (registeredMetrics.contains(metricKey)) {
            return;
        }
        
        HttpInfo httpInfo = httpInfoCache.get(url);
        if (httpInfo == null) {
            return;
        }
        
        // 解析URL获取主机和路径信息
        String host = extractHost(url);
        String scheme = extractScheme(url);
        
        Tags tags = Tags.of(
            "url", url,
            "host", host,
            "scheme", scheme
        );
        
        // HTTP 服务是否可用 (1=可用, 0=不可用)
        Gauge.builder("http_available", httpInfoCache, cache -> {
            HttpInfo info = cache.get(url);
            return info != null && info.isAvailable() ? 1.0 : 0.0;
        })
        .description("HTTP service availability (1=available, 0=unavailable)")
        .tags(tags)
        .register(meterRegistry);
        
        // HTTP 状态码
        Gauge.builder("http_status_code", httpInfoCache, cache -> {
            HttpInfo info = cache.get(url);
            return info != null ? (double) info.getStatusCode() : 0.0;
        })
        .description("HTTP response status code")
        .tags(tags)
        .register(meterRegistry);
        
        // HTTP 响应时间（毫秒）
        Gauge.builder("http_response_time_ms", httpInfoCache, cache -> {
            HttpInfo info = cache.get(url);
            return info != null ? (double) info.getResponseTimeMs() : 0.0;
        })
        .description("HTTP response time in milliseconds")
        .tags(tags)
        .register(meterRegistry);
        
        // HTTP 内容长度（字节）
        Gauge.builder("http_content_length_bytes", httpInfoCache, cache -> {
            HttpInfo info = cache.get(url);
            return info != null ? (double) info.getContentLength() : 0.0;
        })
        .description("HTTP response content length in bytes")
        .tags(tags)
        .register(meterRegistry);
        
        // HTTP 服务状态 (1=可用, 0=不可用, -1=错误)
        Gauge.builder("http_status", httpInfoCache, cache -> {
            HttpInfo info = cache.get(url);
            return info != null ? getStatusValue(info) : -1.0;
        })
        .description("HTTP service status (1=available, 0=unavailable, -1=error)")
        .tags(tags)
        .register(meterRegistry);
        
        // 最后检查时间戳
        Gauge.builder("http_last_checked_timestamp", httpInfoCache, cache -> {
            HttpInfo info = cache.get(url);
            if (info != null && info.getLastChecked() != null) {
                return (double) info.getLastChecked().atZone(java.time.ZoneId.systemDefault()).toEpochSecond();
            }
            return 0.0;
        })
        .description("Timestamp of last HTTP check")
        .tags(tags)
        .register(meterRegistry);
        
        registeredMetrics.add(metricKey);
    }
    
    private String extractHost(String url) {
        try {
            URL urlObj = new URL(url);
            return urlObj.getHost();
        } catch (Exception e) {
            return "unknown";
        }
    }
    
    private String extractScheme(String url) {
        try {
            URL urlObj = new URL(url);
            return urlObj.getProtocol();
        } catch (Exception e) {
            return "unknown";
        }
    }
    
    private double getStatusValue(HttpInfo httpInfo) {
        if (httpInfo.getStatus() == null) {
            return -1.0; // ERROR
        }
        
        switch (httpInfo.getStatus()) {
            case "AVAILABLE":
                return 1.0;
            case "UNAVAILABLE":
                return 0.0;
            default:
                return -1.0; // ERROR
        }
    }
    
    public Map<String, HttpInfo> getHttpInfoCache() {
        return new ConcurrentHashMap<>(httpInfoCache);
    }
}