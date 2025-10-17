package io.github.devops.exporter.domain;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DomainMetricsService {
    
    private final MeterRegistry meterRegistry;
    private final Map<String, DomainInfo> domainInfoCache = new ConcurrentHashMap<>();
    private final Set<String> registeredMetrics = ConcurrentHashMap.newKeySet();
    
    public DomainMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }
    
    public void updateDomainMetrics(DomainInfo domainInfo) {
        String domain = domainInfo.getDomain();
        domainInfoCache.put(domain, domainInfo);
        
        // 注册指标（只注册一次）
        registerDomainMetrics(domain);
    }
    
    private void registerDomainMetrics(String domain) {
        String metricKey = "domain_metrics_" + domain.replace(".", "_");
        
        // 避免重复注册
        if (registeredMetrics.contains(metricKey)) {
            return;
        }
        
        Tags tags = Tags.of("domain", domain);
        
        // 域名过期剩余天数 (-999 表示查询失败)
        Gauge.builder("domain_expiration_days", domainInfoCache, cache -> {
            DomainInfo info = cache.get(domain);
            return info != null ? (double) info.getDaysUntilExpiration() : -999.0;
        })
        .description("Days until domain registration expires (-999=query failed)")
        .tags(tags)
        .register(meterRegistry);
        
        // 域名状态指标 (0=正常, 1=警告, 2=过期, 3=错误)
        Gauge.builder("domain_status", domainInfoCache, cache -> {
            DomainInfo info = cache.get(domain);
            return info != null ? getStatusValue(info) : 3.0;
        })
        .description("Domain registration status (0=valid, 1=warning, 2=expired, 3=error)")
        .tags(tags)
        .register(meterRegistry);
        
        // 域名是否过期 (0=未过期, 1=已过期)
        Gauge.builder("domain_expired", domainInfoCache, cache -> {
            DomainInfo info = cache.get(domain);
            return info != null && info.isExpired() ? 1.0 : 0.0;
        })
        .description("Whether domain registration is expired (0=not expired, 1=expired)")
        .tags(tags)
        .register(meterRegistry);
        
        // 域名是否在警告期 (0=正常, 1=警告)
        Gauge.builder("domain_warning", domainInfoCache, cache -> {
            DomainInfo info = cache.get(domain);
            return info != null && info.isWarning() ? 1.0 : 0.0;
        })
        .description("Whether domain registration is in warning period (0=normal, 1=warning)")
        .tags(tags)
        .register(meterRegistry);
        
        // 最后检查时间戳
        Gauge.builder("domain_last_checked_timestamp", domainInfoCache, cache -> {
            DomainInfo info = cache.get(domain);
            if (info != null && info.getLastChecked() != null) {
                return (double) info.getLastChecked().atZone(java.time.ZoneId.systemDefault()).toEpochSecond();
            }
            return 0.0;
        })
        .description("Timestamp of last domain check")
        .tags(tags)
        .register(meterRegistry);
        
        registeredMetrics.add(metricKey);
    }
    
    private double getStatusValue(DomainInfo domainInfo) {
        if (domainInfo.getStatus() == null) {
            return 3.0; // ERROR
        }
        
        switch (domainInfo.getStatus()) {
            case "VALID":
                return 0.0;
            case "WARNING":
                return 1.0;
            case "EXPIRED":
                return 2.0;
            default:
                return 3.0; // ERROR
        }
    }
    
    public Map<String, DomainInfo> getDomainInfoCache() {
        return new ConcurrentHashMap<>(domainInfoCache);
    }
}