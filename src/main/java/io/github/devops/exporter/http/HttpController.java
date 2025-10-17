package io.github.devops.exporter.http;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/http")
public class HttpController {
    
    private final HttpMonitorScheduler scheduler;
    private final HttpMetricsService metricsService;
    
    public HttpController(HttpMonitorScheduler scheduler,
                         HttpMetricsService metricsService) {
        this.scheduler = scheduler;
        this.metricsService = metricsService;
    }
    
    @PostMapping("/check")
    public ResponseEntity<String> triggerCheck() {
        scheduler.checkAllHttpServices();
        return ResponseEntity.ok("HTTP service availability check triggered");
    }
    
    @GetMapping("/status")
    public ResponseEntity<Map<String, HttpInfo>> getStatus() {
        return ResponseEntity.ok(metricsService.getHttpInfoCache());
    }
    
    @GetMapping("/status/{urlHash}")
    public ResponseEntity<HttpInfo> getHttpStatus(@PathVariable String urlHash) {
        // 由于URL包含特殊字符，使用hashCode作为路径参数
        HttpInfo httpInfo = metricsService.getHttpInfoCache().values().stream()
            .filter(info -> String.valueOf(info.getUrl().hashCode()).equals(urlHash))
            .findFirst()
            .orElse(null);
        
        if (httpInfo != null) {
            return ResponseEntity.ok(httpInfo);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}