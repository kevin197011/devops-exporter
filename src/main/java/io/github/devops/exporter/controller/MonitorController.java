package io.github.devops.exporter.controller;

import io.github.devops.exporter.domain.DomainMetricsService;
import io.github.devops.exporter.http.HttpMetricsService;
import io.github.devops.exporter.http.HttpMonitorScheduler;
import io.github.devops.exporter.port.PortMetricsService;
import io.github.devops.exporter.port.PortMonitorScheduler;
import io.github.devops.exporter.domain.DomainMonitorScheduler;
import io.github.devops.exporter.ssl.SslMetricsService;
import io.github.devops.exporter.ssl.SslMonitorScheduler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/monitor")
public class MonitorController {
    
    private final DomainMonitorScheduler domainScheduler;
    private final SslMonitorScheduler sslScheduler;
    private final PortMonitorScheduler portScheduler;
    private final HttpMonitorScheduler httpScheduler;
    
    private final DomainMetricsService domainMetricsService;
    private final SslMetricsService sslMetricsService;
    private final PortMetricsService portMetricsService;
    private final HttpMetricsService httpMetricsService;
    
    public MonitorController(DomainMonitorScheduler domainScheduler,
                            SslMonitorScheduler sslScheduler,
                            PortMonitorScheduler portScheduler,
                            HttpMonitorScheduler httpScheduler,
                            DomainMetricsService domainMetricsService,
                            SslMetricsService sslMetricsService,
                            PortMetricsService portMetricsService,
                            HttpMetricsService httpMetricsService) {
        this.domainScheduler = domainScheduler;
        this.sslScheduler = sslScheduler;
        this.portScheduler = portScheduler;
        this.httpScheduler = httpScheduler;
        this.domainMetricsService = domainMetricsService;
        this.sslMetricsService = sslMetricsService;
        this.portMetricsService = portMetricsService;
        this.httpMetricsService = httpMetricsService;
    }
    
    @PostMapping("/check/all")
    public ResponseEntity<String> triggerAllChecks() {
        domainScheduler.checkAllDomains();
        sslScheduler.checkAllSslCertificates();
        portScheduler.checkAllPorts();
        httpScheduler.checkAllHttpServices();
        return ResponseEntity.ok("All checks triggered (Domain WHOIS + SSL Certificate + Port Connectivity + HTTP Availability)");
    }
    
    @GetMapping("/status/summary")
    public ResponseEntity<Map<String, Object>> getStatusSummary() {
        Map<String, Object> summary = new HashMap<>();
        
        // 域名监控统计
        Map<String, Object> domainStats = new HashMap<>();
        domainStats.put("total", domainMetricsService.getDomainInfoCache().size());
        domainStats.put("expired", domainMetricsService.getDomainInfoCache().values().stream()
            .mapToLong(info -> info.isExpired() ? 1 : 0).sum());
        domainStats.put("warning", domainMetricsService.getDomainInfoCache().values().stream()
            .mapToLong(info -> info.isWarning() ? 1 : 0).sum());
        summary.put("domain", domainStats);
        
        // SSL 监控统计
        Map<String, Object> sslStats = new HashMap<>();
        sslStats.put("total", sslMetricsService.getSslInfoCache().size());
        sslStats.put("expired", sslMetricsService.getSslInfoCache().values().stream()
            .mapToLong(info -> info.isExpired() ? 1 : 0).sum());
        sslStats.put("warning", sslMetricsService.getSslInfoCache().values().stream()
            .mapToLong(info -> info.isWarning() ? 1 : 0).sum());
        summary.put("ssl", sslStats);
        
        // 端口监控统计
        Map<String, Object> portStats = new HashMap<>();
        portStats.put("total", portMetricsService.getPortInfoCache().size());
        portStats.put("open", portMetricsService.getPortInfoCache().values().stream()
            .mapToLong(info -> info.isOpen() ? 1 : 0).sum());
        portStats.put("closed", portMetricsService.getPortInfoCache().values().stream()
            .mapToLong(info -> !info.isOpen() ? 1 : 0).sum());
        summary.put("port", portStats);
        
        // HTTP 监控统计
        Map<String, Object> httpStats = new HashMap<>();
        httpStats.put("total", httpMetricsService.getHttpInfoCache().size());
        httpStats.put("available", httpMetricsService.getHttpInfoCache().values().stream()
            .mapToLong(info -> info.isAvailable() ? 1 : 0).sum());
        httpStats.put("unavailable", httpMetricsService.getHttpInfoCache().values().stream()
            .mapToLong(info -> !info.isAvailable() ? 1 : 0).sum());
        summary.put("http", httpStats);
        
        return ResponseEntity.ok(summary);
    }
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> getHealth() {
        Map<String, String> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "DevOps Exporter");
        health.put("version", "1.0.0");
        return ResponseEntity.ok(health);
    }
}