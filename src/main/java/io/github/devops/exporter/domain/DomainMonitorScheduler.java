package io.github.devops.exporter.domain;

import io.github.devops.exporter.config.DomainMonitorProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
@EnableScheduling
@EnableAsync
public class DomainMonitorScheduler {
    
    private static final Logger logger = LoggerFactory.getLogger(DomainMonitorScheduler.class);
    
    private final DomainMonitorProperties properties;
    private final DomainCheckService domainCheckService;
    private final DomainMetricsService metricsService;
    
    public DomainMonitorScheduler(DomainMonitorProperties properties,
                                 DomainCheckService domainCheckService,
                                 DomainMetricsService metricsService) {
        this.properties = properties;
        this.domainCheckService = domainCheckService;
        this.metricsService = metricsService;
    }
    
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (properties.isEnabled()) {
            logger.info("Domain monitor is enabled, starting initial check...");
            checkAllDomains();
        } else {
            logger.info("Domain monitor is disabled");
        }
    }
    
    @Scheduled(fixedRateString = "#{${domain.monitor.check-interval} * 1000}")
    public void scheduledCheck() {
        if (properties.isEnabled()) {
            logger.info("Starting scheduled domain check...");
            checkAllDomains();
        }
    }
    
    @Async
    public void checkAllDomains() {
        List<String> domains = properties.getDomains();
        if (domains == null || domains.isEmpty()) {
            logger.warn("No domains configured for monitoring");
            return;
        }
        
        logger.info("Checking {} domains", domains.size());
        
        List<CompletableFuture<Void>> futures = domains.stream()
            .map(domain -> 
                domainCheckService.checkDomainAsync(domain)
                    .thenAccept(domainInfo -> {
                        metricsService.updateDomainMetrics(domainInfo);
                        logDomainStatus(domainInfo);
                    })
                    .exceptionally(throwable -> {
                        logger.error("Error checking domain {}: {}", 
                            domain, throwable.getMessage());
                        
                        // 创建错误状态的 DomainInfo
                        DomainInfo errorInfo = new DomainInfo(domain);
                        errorInfo.setStatus("ERROR");
                        errorInfo.setError(throwable.getMessage());
                        errorInfo.setDaysUntilExpiration(-999);
                        metricsService.updateDomainMetrics(errorInfo);
                        
                        return null;
                    })
            )
            .toList();
        
        // 等待所有检查完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenRun(() -> logger.info("Domain check completed"))
            .exceptionally(throwable -> {
                logger.error("Error during domain check: {}", throwable.getMessage());
                return null;
            });
    }
    
    private void logDomainStatus(DomainInfo domainInfo) {
        String domain = domainInfo.getDomain();
        String status = domainInfo.getStatus();
        
        if ("ERROR".equals(status)) {
            logger.error("Domain {} WHOIS check failed: {}", domain, domainInfo.getError());
        } else if ("EXPIRED".equals(status)) {
            logger.warn("Domain {} registration has EXPIRED!", domain);
        } else if ("WARNING".equals(status)) {
            logger.warn("Domain {} registration expires in {} days", 
                domain, domainInfo.getDaysUntilExpiration());
        } else {
            logger.info("Domain {} registration is valid, expires in {} days", 
                domain, domainInfo.getDaysUntilExpiration());
        }
    }
}