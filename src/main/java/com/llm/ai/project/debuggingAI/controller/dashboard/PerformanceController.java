package com.llm.ai.project.debuggingAI.controller.dashboard;

import com.llm.ai.core.aspect.PerformanceAnalyzer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/performance")
public class PerformanceController {

    @Autowired
    PerformanceAnalyzer performanceAnalyzer;

    @GetMapping("/performance-metrics")
    public Map<String, Object> getPerformanceMetrics() {
        Map<String, PerformanceAnalyzer.ErrorMetric> metrics = performanceAnalyzer.getErrorMetrics();
        List<Map<String, Object>> metricList = new ArrayList<>();
        metrics.forEach((key, metric) -> {
            Map<String, Object> item = new HashMap<>();
            item.put("error", key);
            item.put("occurrences", metric.getOccurrences());
            item.put("avgDuration", metric.getAvgDuration() + " ms");
            item.put("maxDuration", metric.getMaxDuration() + " ms");
            item.put("avgMemory", metric.getAvgMemory() / 1024 + " KB");
            metricList.add(item);
        });

        metricList.sort((a, b) -> Integer.compare(
                (int) b.get("occurrences"), (int) a.get("occurrences")));

        return Map.of(
                "totalTracked", metrics.size(),
                "metrics", metricList
        );
    }

    @GetMapping("/clear-performance")
    public String clearPerformance() {
        performanceAnalyzer.clearMetrics();
        return "✅ Performance metrics cleared";
    }
}
