package io.github.devops.exporter.port;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/port")
public class PortController {
    
    private final PortMonitorScheduler scheduler;
    private final PortMetricsService metricsService;
    
    public PortController(PortMonitorScheduler scheduler,
                         PortMetricsService metricsService) {
        this.scheduler = scheduler;
        this.metricsService = metricsService;
    }
    
    @PostMapping("/check")
    public ResponseEntity<String> triggerCheck() {
        scheduler.checkAllPorts();
        return ResponseEntity.ok("Port connectivity check triggered");
    }
    
    @GetMapping("/status")
    public ResponseEntity<Map<String, PortInfo>> getStatus() {
        return ResponseEntity.ok(metricsService.getPortInfoCache());
    }
    
    @GetMapping("/status/{target}")
    public ResponseEntity<PortInfo> getPortStatus(@PathVariable String target) {
        // 处理路径参数中的冒号，例如 1.1.1.1:80 -> 1.1.1.1%3A80
        target = target.replace("%3A", ":");
        PortInfo portInfo = metricsService.getPortInfoCache().get(target);
        if (portInfo != null) {
            return ResponseEntity.ok(portInfo);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}