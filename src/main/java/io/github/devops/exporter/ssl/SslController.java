package io.github.devops.exporter.ssl;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ssl")
public class SslController {
    
    private final SslMonitorScheduler scheduler;
    private final SslMetricsService metricsService;
    
    public SslController(SslMonitorScheduler scheduler,
                        SslMetricsService metricsService) {
        this.scheduler = scheduler;
        this.metricsService = metricsService;
    }
    
    @PostMapping("/check")
    public ResponseEntity<String> triggerCheck() {
        scheduler.checkAllSslCertificates();
        return ResponseEntity.ok("SSL certificate check triggered");
    }
    
    @GetMapping("/status")
    public ResponseEntity<Map<String, SslCertificateInfo>> getStatus() {
        return ResponseEntity.ok(metricsService.getSslInfoCache());
    }
    
    @GetMapping("/status/{domain}")
    public ResponseEntity<SslCertificateInfo> getSslStatus(@PathVariable String domain) {
        SslCertificateInfo sslInfo = metricsService.getSslInfoCache().get(domain);
        if (sslInfo != null) {
            return ResponseEntity.ok(sslInfo);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}