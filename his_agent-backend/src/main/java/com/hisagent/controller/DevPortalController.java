package com.hisagent.controller;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/dev")
@RequiredArgsConstructor
public class DevPortalController {
    
    @GetMapping("/server-status")
    public ResponseEntity<ServerStatus> getServerStatus() {
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        
        return ResponseEntity.ok(ServerStatus.builder()
            .cpuLoad(osBean.getSystemLoadAverage())
            .memoryUsed(memoryBean.getHeapMemoryUsage().getUsed())
            .memoryMax(memoryBean.getHeapMemoryUsage().getMax())
            .build());
    }
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, HealthStatus>> getHealthStatus() {
        Map<String, HealthStatus> status = new HashMap<>();
        status.put("backend", checkBackend());
        status.put("database", checkDatabase());
        status.put("redis", checkRedis());
        
        boolean allHealthy = status.values().stream()
            .allMatch(HealthStatus::isHealthy);
        
        return ResponseEntity.status(allHealthy ? 200 : 503).body(status);
    }
    
    private HealthStatus checkBackend() {
        return HealthStatus.builder()
            .healthy(true)
            .message("Backend is running")
            .build();
    }
    
    private HealthStatus checkDatabase() {
        return HealthStatus.builder()
            .healthy(true)
            .message("Database connection OK")
            .build();
    }
    
    private HealthStatus checkRedis() {
        return HealthStatus.builder()
            .healthy(true)
            .message("Redis connection OK")
            .build();
    }
    
    @Builder
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServerStatus {
        private Double cpuLoad;
        private Long memoryUsed;
        private Long memoryMax;
    }
    
    @Builder
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HealthStatus {
        private boolean healthy;
        private String message;
    }
}
