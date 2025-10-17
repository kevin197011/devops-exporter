package io.github.devops.exporter.http;

import io.github.devops.exporter.config.HttpMonitorProperties;
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
public class HttpMonitorScheduler {
    
    private static final Logger logger = LoggerFactory.getLogger(HttpMonitorScheduler.class);
    
    private final HttpMonitorProperties properties;
    private final HttpCheckService httpCheckService;
    private final HttpMetricsService metricsService;
    
    public HttpMonitorScheduler(HttpMonitorProperties properties,
                               HttpCheckService httpCheckService,
                               HttpMetricsService metricsService) {
        this.properties = properties;
        this.httpCheckService = httpCheckService;
        this.metricsService = metricsService;
    }
    
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (properties.isEnabled()) {
            logger.info("HTTP monitor is enabled, starting initial check...");
            checkAllHttpServices();
        } else {
            logger.info("HTTP monitor is disabled");
        }
    }
    
    @Scheduled(fixedRateString = "#{${http.monitor.check-interval} * 1000}")
    public void scheduledCheck() {
        if (properties.isEnabled()) {
            logger.info("Starting scheduled HTTP service check...");
            checkAllHttpServices();
        }
    }
    
    @Async
    public void checkAllHttpServices() {
        List<String> urls = properties.getUrls();
        if (urls == null || urls.isEmpty()) {
            logger.warn("No URLs configured for HTTP monitoring");
            return;
        }
        
        logger.info("Checking {} HTTP services", urls.size());
        
        List<CompletableFuture<Void>> futures = urls.stream()
            .map(url -> 
                httpCheckService.checkHttpAsync(url)
                    .thenAccept(httpInfo -> {
                        metricsService.updateHttpMetrics(httpInfo);
                        logHttpStatus(httpInfo);
                    })
                    .exceptionally(throwable -> {
                        logger.error("Error checking HTTP service {}: {}", 
                            url, throwable.getMessage());
                        
                        // 创建错误状态的 HttpInfo
                        HttpInfo errorInfo = new HttpInfo(url);
                        errorInfo.setStatus("ERROR");
                        errorInfo.setError(throwable.getMessage());
                        errorInfo.setAvailable(false);
                        metricsService.updateHttpMetrics(errorInfo);
                        
                        return null;
                    })
            )
            .toList();
        
        // 等待所有检查完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenRun(() -> logger.info("HTTP service check completed"))
            .exceptionally(throwable -> {
                logger.error("Error during HTTP service check: {}", throwable.getMessage());
                return null;
            });
    }
    
    private void logHttpStatus(HttpInfo httpInfo) {
        String url = httpInfo.getUrl();
        String status = httpInfo.getStatus();
        
        if ("ERROR".equals(status)) {
            logger.error("HTTP check for {} failed: {}", url, httpInfo.getError());
        } else if ("UNAVAILABLE".equals(status)) {
            logger.warn("HTTP service {} is UNAVAILABLE: {} {}", 
                url, httpInfo.getStatusCode(), httpInfo.getStatusMessage());
        } else if ("AVAILABLE".equals(status)) {
            logger.info("HTTP service {} is AVAILABLE: {} {} ({}ms)", 
                url, httpInfo.getStatusCode(), httpInfo.getStatusMessage(), httpInfo.getResponseTimeMs());
        }
    }
}