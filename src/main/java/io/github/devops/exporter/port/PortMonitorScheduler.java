package io.github.devops.exporter.port;

import io.github.devops.exporter.config.PortMonitorProperties;
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
public class PortMonitorScheduler {
    
    private static final Logger logger = LoggerFactory.getLogger(PortMonitorScheduler.class);
    
    private final PortMonitorProperties properties;
    private final PortCheckService portCheckService;
    private final PortMetricsService metricsService;
    
    public PortMonitorScheduler(PortMonitorProperties properties,
                               PortCheckService portCheckService,
                               PortMetricsService metricsService) {
        this.properties = properties;
        this.portCheckService = portCheckService;
        this.metricsService = metricsService;
    }
    
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (properties.isEnabled()) {
            logger.info("Port monitor is enabled, starting initial check...");
            checkAllPorts();
        } else {
            logger.info("Port monitor is disabled");
        }
    }
    
    @Scheduled(fixedRateString = "#{${port.monitor.check-interval} * 1000}")
    public void scheduledCheck() {
        if (properties.isEnabled()) {
            logger.info("Starting scheduled port check...");
            checkAllPorts();
        }
    }
    
    @Async
    public void checkAllPorts() {
        List<String> ports = properties.getPorts();
        if (ports == null || ports.isEmpty()) {
            logger.warn("No ports configured for monitoring");
            return;
        }
        
        logger.info("Checking {} ports", ports.size());
        
        List<CompletableFuture<Void>> futures = ports.stream()
            .map(target -> 
                portCheckService.checkPortAsync(target)
                    .thenAccept(portInfo -> {
                        metricsService.updatePortMetrics(portInfo);
                        logPortStatus(portInfo);
                    })
                    .exceptionally(throwable -> {
                        logger.error("Error checking port {}: {}", 
                            target, throwable.getMessage());
                        
                        // 创建错误状态的 PortInfo
                        PortInfo errorInfo = new PortInfo(target);
                        errorInfo.setStatus("ERROR");
                        errorInfo.setError(throwable.getMessage());
                        errorInfo.setOpen(false);
                        metricsService.updatePortMetrics(errorInfo);
                        
                        return null;
                    })
            )
            .toList();
        
        // 等待所有检查完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenRun(() -> logger.info("Port check completed"))
            .exceptionally(throwable -> {
                logger.error("Error during port check: {}", throwable.getMessage());
                return null;
            });
    }
    
    private void logPortStatus(PortInfo portInfo) {
        String target = portInfo.getTarget();
        String status = portInfo.getStatus();
        
        if ("ERROR".equals(status) || "INVALID_FORMAT".equals(status)) {
            logger.error("Port check for {} failed: {}", target, portInfo.getError());
        } else if ("CLOSED".equals(status)) {
            logger.warn("Port {} is CLOSED", target);
        } else if ("OPEN".equals(status)) {
            logger.info("Port {} is OPEN (response time: {}ms)", 
                target, portInfo.getResponseTimeMs());
        }
    }
}