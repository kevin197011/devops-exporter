package io.github.devops.exporter.controller;

import io.github.devops.exporter.domain.DomainInfo;
import io.github.devops.exporter.metrics.DomainMetricsService;
import io.github.devops.exporter.scheduler.DomainMonitorScheduler;
import io.github.devops.exporter.ssl.SslCertificateInfo;
import io.github.devops.exporter.ssl.SslMetricsService;
import io.github.devops.exporter.ssl.SslMonitorScheduler;
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
    
    public DomainMonitorController(DomainMonitorScheduler domainScheduler, 
                                  DomainMetricsService domainMetricsService,
                                  SslMonitorScheduler sslScheduler,
                                  SslMetricsService sslMetricsService) {
        this.domainScheduler = domainScheduler;
        this.domainMetricsService = domainMetricsService;
        this.sslScheduler = sslScheduler;
        this.sslMetricsService = sslMetricsService;
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
    
    // Combined API
    @PostMapping("/check/all")
    public ResponseEntity<String> triggerAllChecks() {
        domainScheduler.checkAllDomains();
        sslScheduler.checkAllSslCertificates();
        return ResponseEntity.ok("All checks triggered (Domain WHOIS + SSL Certificate)");
    }
}