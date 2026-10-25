package com.dotechly.gotechly.gateway.web.rest;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/admin/metrics")
public class MetricsResource {

    private static final Logger log = LoggerFactory.getLogger(MetricsResource.class);

    private final MeterRegistry meterRegistry;

    public MetricsResource(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @GetMapping("/gateway")
    public List<Map<String, Object>> getGatewayMetrics() {
        Map<String, Map<String, Object>> routeMetrics = new LinkedHashMap<>();

        try {
            for (Meter meter : meterRegistry.getMeters()) {
                if (!meter.getId().getName().equals("gateway.request.duration")) continue;
                if (!(meter instanceof Timer timer)) continue;

                String routeId = timer.getId().getTag("routeId");
                if (routeId == null) routeId = "unknown";

                long count = timer.count();
                double avgMs = timer.mean(TimeUnit.MILLISECONDS);
                double maxMs = timer.max(TimeUnit.MILLISECONDS);

                Map<String, Object> existing = routeMetrics.get(routeId);
                if (existing == null) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("routeId", routeId);
                    m.put("totalRequests", count);
                    m.put("avgLatencyMs", avgMs);
                    m.put("maxLatencyMs", maxMs);
                    m.put("errorCount", 0L);
                    routeMetrics.put(routeId, m);
                    existing = m;
                } else {
                    existing.put("totalRequests", (long) existing.get("totalRequests") + count);
                    if (maxMs > (double) existing.get("maxLatencyMs")) {
                        existing.put("maxLatencyMs", maxMs);
                    }
                }

                String status = timer.getId().getTag("status");
                if (status != null && (status.startsWith("4") || status.startsWith("5"))) {
                    existing.put("errorCount", (long) existing.get("errorCount") + count);
                }
            }
        } catch (Exception e) {
            log.error("Failed to collect gateway metrics", e);
        }

        return new ArrayList<>(routeMetrics.values());
    }

    @GetMapping("/gateway/summary")
    public Map<String, Object> getGatewaySummary() {
        long totalRequests = 0;
        long totalErrors = 0;
        double totalLatency = 0;
        int timerCount = 0;

        try {
            for (Meter meter : meterRegistry.getMeters()) {
                if (!meter.getId().getName().equals("gateway.request.duration")) continue;
                if (!(meter instanceof Timer timer)) continue;

                totalRequests += timer.count();
                totalLatency += timer.mean(TimeUnit.MILLISECONDS);
                timerCount++;

                String status = timer.getId().getTag("status");
                if (status != null && (status.startsWith("4") || status.startsWith("5"))) {
                    totalErrors += timer.count();
                }
            }
        } catch (Exception e) {
            log.error("Failed to collect gateway summary", e);
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalRequests", totalRequests);
        summary.put("totalErrors", totalErrors);
        summary.put("avgLatencyMs", timerCount > 0 ? totalLatency / timerCount : 0.0);
        summary.put("successRate", totalRequests > 0 ? ((double) (totalRequests - totalErrors) / totalRequests) * 100 : 100.0);
        return summary;
    }
}
