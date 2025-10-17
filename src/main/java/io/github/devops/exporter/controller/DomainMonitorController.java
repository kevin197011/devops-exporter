package io.github.devops.exporter.controller;

import io.github.devops.exporter.domain.DomainInfo;
import io.github.devops.exporter.metrics.DomainMetricsService;
import io.github.devops.exporter.scheduler.DomainMonitorScheduler;
import io.github.devops.exporter.ssl.SslCertificateInfo;
import io.github.devops.exporter.ssl.SslMetricsService;
import io.github.devops.exporter.ssl.SslMonitorScheduler;
import io.github.devops.exporter.port.PortInfo;
import io.github.devops.exporter.port.PortMetricsService;
import io.github.devops.exporter.port.PortMonitorScheduler;
import io.github.devops.exporter.http.HttpInfo;
import io.github.devops.exporter.http.HttpMetricsService;
import io.github.devops.exporter.http.HttpMonitorScheduler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class DomainMonitorController {
    
    private final DomainMonitorScheduler domainScheduler;
    private final DomainMetricsService domainMetricsService;
    private final SslMonitorScheduler sslScheduler;
    private final SslMetricsService sslMetricsService;
    private final PortMonitorScheduler portScheduler;
    private final PortMetricsService portMetricsService;
    private final HttpMonitorScheduler httpScheduler;
    private final HttpMetricsService httpMetricsService;
    
    public DomainMonitorController(DomainMonitorScheduler domainScheduler, 
                                  DomainMetricsService domainMetricsService,
                                  SslMonitorScheduler sslScheduler,
                                  SslMetricsService sslMetricsService,
                                  PortMonitorScheduler portScheduler,
                                  PortMetricsService portMetricsService,
                                  HttpMonitorScheduler httpScheduler,
                                  HttpMetricsService httpMetricsService) {
        this.domainScheduler = domainScheduler;
        this.domainMetricsService = domainMetricsService;
        this.sslScheduler = sslScheduler;
        this.sslMetricsService = sslMetricsService;
        this.portScheduler = portScheduler;
        this.portMetricsService = portMetricsService;
        this.httpScheduler = httpScheduler;
        this.httpMetricsService = httpMetricsService;
    }
    
    // Domain WHOIS API
    @PostMapping("/domain/check")
    public ResponseEntity<String> triggerDomainCheck() {
        domainScheduler.checkAllDomains();
        return ResponseEntity.ok("Domain WHOIS check triggered");
    }
    
    @GetMapping("/domain/status")
    public ResponseEntity<Map<String, DomainInfo>> getDomainStatus() {
        return ResponseEntity.ok(domainMetricsService.getDomainInfoCache());
    }
    
    @GetMapping("/domain/status/{domain}")
    public ResponseEntity<DomainInfo> getDomainStatus(@PathVariable String domain) {
        DomainInfo domainInfo = domainMetricsService.getDomainInfoCache().get(domain);
        if (domainInfo != null) {
            return ResponseEntity.ok(domainInfo);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    // SSL Certificate API
    @PostMapping("/ssl/check")
    public ResponseEntity<String> triggerSslCheck() {
        sslScheduler.checkAllSslCertificates();
        return ResponseEntity.ok("SSL certificate check triggered");
    }
    
    @GetMapping("/ssl/status")
    public ResponseEntity<Map<String, SslCertificateInfo>> getSslStatus() {
        return ResponseEntity.ok(sslMetricsService.getSslInfoCache());
    }
    
    @GetMapping("/ssl/status/{domain}")
    public ResponseEntity<SslCertificateInfo> getSslStatus(@PathVariable String domain) {
        SslCertificateInfo sslInfo = sslMetricsService.getSslInfoCache().get(domain);
        if (sslInfo != null) {
            return ResponseEntity.ok(sslInfo);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    // Port Monitoring API
    @PostMapping("/port/check")
    public ResponseEntity<String> triggerPortCheck() {
        portScheduler.checkAllPorts();
        return ResponseEntity.ok("Port connectivity check triggered");
    }
    
    @GetMapping("/port/status")
    public ResponseEntity<Map<String, PortInfo>> getPortStatus() {
        return ResponseEntity.ok(portMetricsService.getPortInfoCache());
    }
    
    @GetMapping("/port/status/{target}")
    public ResponseEntity<PortInfo> getPortStatus(@PathVariable String target) {
        // 处理路径参数中的冒号，例如 1.1.1.1:80 -> 1.1.1.1%3A80
        target = target.replace("%3A", ":");
        PortInfo portInfo = portMetricsService.getPortInfoCache().get(target);
        if (portInfo != null) {
            return ResponseEntity.ok(portInfo);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    // HTTP Service Monitoring API
    @PostMapping("/http/check")
    public ResponseEntity<String> triggerHttpCheck() {
        httpScheduler.checkAllHttpServices();
        return ResponseEntity.ok("HTTP service availability check triggered");
    }
    
    @GetMapping("/http/status")
    public ResponseEntity<Map<String, HttpInfo>> getHttpStatus() {
        return ResponseEntity.ok(httpMetricsService.getHttpInfoCache());
    }
    
    @GetMapping("/http/status/{urlHash}")
    public ResponseEntity<HttpInfo> getHttpStatus(@PathVariable String urlHash) {
        // 由于URL包含特殊字符，使用hashCode作为路径参数
        HttpInfo httpInfo = httpMetricsService.getHttpInfoCache().values().stream()
            .filter(info -> String.valueOf(info.getUrl().hashCode()).equals(urlHash))
            .findFirst()
            .orElse(null);
        
        if (httpInfo != null) {
            return ResponseEntity.ok(httpInfo);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    // Combined API
    @PostMapping("/check/all")
    public ResponseEntity<String> triggerAllChecks() {
        domainScheduler.checkAllDomains();
        sslScheduler.checkAllSslCertificates();
        portScheduler.checkAllPorts();
        httpScheduler.checkAllHttpServices();
        return ResponseEntity.ok("All checks triggered (Domain WHOIS + SSL Certificate + Port Connectivity + HTTP Availability)");
    }
}