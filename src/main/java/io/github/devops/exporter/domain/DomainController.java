package io.github.devops.exporter.domain;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/domain")
public class DomainController {
    
    private final DomainMonitorScheduler scheduler;
    private final DomainMetricsService metricsService;
    
    public DomainController(DomainMonitorScheduler scheduler, 
                           DomainMetricsService metricsService) {
        this.scheduler = scheduler;
        this.metricsService = metricsService;
    }
    
    @PostMapping("/check")
    public ResponseEntity<String> triggerCheck() {
        scheduler.checkAllDomains();
        return ResponseEntity.ok("Domain WHOIS check triggered");
    }
    
    @GetMapping("/status")
    public ResponseEntity<Map<String, DomainInfo>> getStatus() {
        return ResponseEntity.ok(metricsService.getDomainInfoCache());
    }
    
    @GetMapping("/status/{domain}")
    public ResponseEntity<DomainInfo> getDomainStatus(@PathVariable String domain) {
        DomainInfo domainInfo = metricsService.getDomainInfoCache().get(domain);
        if (domainInfo != null) {
            return ResponseEntity.ok(domainInfo);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}