package io.github.devops.exporter.port;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PortMetricsService {
    
    private final MeterRegistry meterRegistry;
    private final Map<String, PortInfo> portInfoCache = new ConcurrentHashMap<>();
    private final Set<String> registeredMetrics = ConcurrentHashMap.newKeySet();
    
    public PortMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }
    
    public void updatePortMetrics(PortInfo portInfo) {
        String target = portInfo.getTarget();
        portInfoCache.put(target, portInfo);
        
        // 注册指标（只注册一次）
        registerPortMetrics(target);
    }
    
    private void registerPortMetrics(String target) {
        String metricKey = "port_metrics_" + target.replace(".", "_").replace(":", "_");
        
        // 避免重复注册
        if (registeredMetrics.contains(metricKey)) {
            return;
        }
        
        PortInfo portInfo = portInfoCache.get(target);
        if (portInfo == null) {
            return;
        }
        
        Tags tags = Tags.of(
            "target", target,
            "host", portInfo.getHost(),
            "port", String.valueOf(portInfo.getPort())
        );
        
        // 端口是否开放 (1=开放, 0=关闭)
        Gauge.builder("port_open", portInfoCache, cache -> {
            PortInfo info = cache.get(target);
            return info != null && info.isOpen() ? 1.0 : 0.0;
        })
        .description("Port availability (1=open, 0=closed)")
        .tags(tags)
        .register(meterRegistry);
        
        // 端口状态 (1=开放, 0=关闭, -1=错误)
        Gauge.builder("port_status", portInfoCache, cache -> {
            PortInfo info = cache.get(target);
            return info != null ? getStatusValue(info) : -1.0;
        })
        .description("Port status (1=open, 0=closed, -1=error)")
        .tags(tags)
        .register(meterRegistry);
        
        // 响应时间（毫秒）
        Gauge.builder("port_response_time_ms", portInfoCache, cache -> {
            PortInfo info = cache.get(target);
            return info != null ? (double) info.getResponseTimeMs() : 0.0;
        })
        .description("Port connection response time in milliseconds")
        .tags(tags)
        .register(meterRegistry);
        
        // 最后检查时间戳
        Gauge.builder("port_last_checked_timestamp", portInfoCache, cache -> {
            PortInfo info = cache.get(target);
            if (info != null && info.getLastChecked() != null) {
                return (double) info.getLastChecked().atZone(java.time.ZoneId.systemDefault()).toEpochSecond();
            }
            return 0.0;
        })
        .description("Timestamp of last port check")
        .tags(tags)
        .register(meterRegistry);
        
        registeredMetrics.add(metricKey);
    }
    
    private double getStatusValue(PortInfo portInfo) {
        if (portInfo.getStatus() == null) {
            return -1.0; // ERROR
        }
        
        switch (portInfo.getStatus()) {
            case "OPEN":
                return 1.0;
            case "CLOSED":
                return 0.0;
            default:
                return -1.0; // ERROR or INVALID_FORMAT
        }
    }
    
    public Map<String, PortInfo> getPortInfoCache() {
        return new ConcurrentHashMap<>(portInfoCache);
    }
}