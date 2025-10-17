package io.github.devops.exporter.ssl;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SslMetricsService {
    
    private final MeterRegistry meterRegistry;
    private final Map<String, SslCertificateInfo> sslInfoCache = new ConcurrentHashMap<>();
    private final Set<String> registeredMetrics = ConcurrentHashMap.newKeySet();
    
    public SslMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }
    
    public void updateSslMetrics(SslCertificateInfo sslInfo) {
        String domain = sslInfo.getDomain();
        sslInfoCache.put(domain, sslInfo);
        
        // 注册指标（只注册一次）
        registerSslMetrics(domain);
    }
    
    private void registerSslMetrics(String domain) {
        String metricKey = "ssl_metrics_" + domain.replace(".", "_");
        
        // 避免重复注册
        if (registeredMetrics.contains(metricKey)) {
            return;
        }
        
        Tags tags = Tags.of("domain", domain);
        
        // SSL 证书过期剩余天数 (-999 表示查询失败)
        Gauge.builder("ssl_certificate_expiration_days", sslInfoCache, cache -> {
            SslCertificateInfo info = cache.get(domain);
            return info != null ? (double) info.getDaysUntilExpiration() : -999.0;
        })
        .description("Days until SSL certificate expires (-999=query failed)")
        .tags(tags)
        .register(meterRegistry);
        
        // SSL 证书状态指标 (0=正常, 1=警告, 2=过期, 3=错误)
        Gauge.builder("ssl_certificate_status", sslInfoCache, cache -> {
            SslCertificateInfo info = cache.get(domain);
            return info != null ? getStatusValue(info) : 3.0;
        })
        .description("SSL certificate status (0=valid, 1=warning, 2=expired, 3=error)")
        .tags(tags)
        .register(meterRegistry);
        
        // SSL 证书是否过期 (0=未过期, 1=已过期)
        Gauge.builder("ssl_certificate_expired", sslInfoCache, cache -> {
            SslCertificateInfo info = cache.get(domain);
            return info != null && info.isExpired() ? 1.0 : 0.0;
        })
        .description("Whether SSL certificate is expired (0=not expired, 1=expired)")
        .tags(tags)
        .register(meterRegistry);
        
        // SSL 证书是否在警告期 (0=正常, 1=警告)
        Gauge.builder("ssl_certificate_warning", sslInfoCache, cache -> {
            SslCertificateInfo info = cache.get(domain);
            return info != null && info.isWarning() ? 1.0 : 0.0;
        })
        .description("Whether SSL certificate is in warning period (0=normal, 1=warning)")
        .tags(tags)
        .register(meterRegistry);
        
        // 最后检查时间戳
        Gauge.builder("ssl_certificate_last_checked_timestamp", sslInfoCache, cache -> {
            SslCertificateInfo info = cache.get(domain);
            if (info != null && info.getLastChecked() != null) {
                return (double) info.getLastChecked().atZone(java.time.ZoneId.systemDefault()).toEpochSecond();
            }
            return 0.0;
        })
        .description("Timestamp of last SSL certificate check")
        .tags(tags)
        .register(meterRegistry);
        
        registeredMetrics.add(metricKey);
    }
    
    private double getStatusValue(SslCertificateInfo sslInfo) {
        if (sslInfo.getStatus() == null) {
            return 3.0; // ERROR
        }
        
        switch (sslInfo.getStatus()) {
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
    
    public Map<String, SslCertificateInfo> getSslInfoCache() {
        return new ConcurrentHashMap<>(sslInfoCache);
    }
}