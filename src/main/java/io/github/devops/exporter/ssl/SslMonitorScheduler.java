package io.github.devops.exporter.ssl;

import io.github.devops.exporter.config.SslMonitorProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
public class SslMonitorScheduler {
    
    private static final Logger logger = LoggerFactory.getLogger(SslMonitorScheduler.class);
    
    private final SslMonitorProperties properties;
    private final SslCheckService sslCheckService;
    private final SslMetricsService metricsService;
    
    public SslMonitorScheduler(SslMonitorProperties properties,
                              SslCheckService sslCheckService,
                              SslMetricsService metricsService) {
        this.properties = properties;
        this.sslCheckService = sslCheckService;
        this.metricsService = metricsService;
    }
    
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (properties.isEnabled()) {
            logger.info("SSL monitor is enabled, starting initial check...");
            checkAllSslCertificates();
        } else {
            logger.info("SSL monitor is disabled");
        }
    }
    
    @Scheduled(fixedRateString = "#{${ssl.monitor.check-interval} * 1000}")
    public void scheduledCheck() {
        if (properties.isEnabled()) {
            logger.info("Starting scheduled SSL certificate check...");
            checkAllSslCertificates();
        }
    }
    
    @Async
    public void checkAllSslCertificates() {
        List<String> domains = properties.getDomains();
        if (domains == null || domains.isEmpty()) {
            logger.warn("No domains configured for SSL monitoring");
            return;
        }
        
        logger.info("Checking SSL certificates for {} domains", domains.size());
        
        List<CompletableFuture<Void>> futures = domains.stream()
            .map(domain -> 
                sslCheckService.checkSslAsync(domain)
                    .thenAccept(sslInfo -> {
                        metricsService.updateSslMetrics(sslInfo);
                        logSslStatus(sslInfo);
                    })
                    .exceptionally(throwable -> {
                        logger.error("Error checking SSL for domain {}: {}", 
                            domain, throwable.getMessage());
                        
                        // 创建错误状态的 SslCertificateInfo
                        SslCertificateInfo errorInfo = new SslCertificateInfo(domain);
                        errorInfo.setStatus("ERROR");
                        errorInfo.setError(throwable.getMessage());
                        errorInfo.setDaysUntilExpiration(-999);
                        metricsService.updateSslMetrics(errorInfo);
                        
                        return null;
                    })
            )
            .toList();
        
        // 等待所有检查完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenRun(() -> logger.info("SSL certificate check completed"))
            .exceptionally(throwable -> {
                logger.error("Error during SSL certificate check: {}", throwable.getMessage());
                return null;
            });
    }
    
    private void logSslStatus(SslCertificateInfo sslInfo) {
        String domain = sslInfo.getDomain();
        String status = sslInfo.getStatus();
        
        if ("ERROR".equals(status)) {
            logger.error("SSL certificate check for {} failed: {}", domain, sslInfo.getError());
        } else if ("EXPIRED".equals(status)) {
            logger.warn("SSL certificate for {} has EXPIRED!", domain);
        } else if ("WARNING".equals(status)) {
            logger.warn("SSL certificate for {} expires in {} days", 
                domain, sslInfo.getDaysUntilExpiration());
        } else {
            logger.info("SSL certificate for {} is valid, expires in {} days", 
                domain, sslInfo.getDaysUntilExpiration());
        }
    }
}