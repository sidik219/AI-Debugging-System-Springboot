package com.llm.ai.core.aspect;

import com.llm.ai.project.debuggingAI.service.AIDebugService;
import jakarta.annotation.PostConstruct;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Aspect
@Component
public class PerformanceAnalyzer {

    private final Map<String, ErrorMetric> errorMetrics = new ConcurrentHashMap<>();

//    @PostConstruct
//    public void init() {
//        System.out.println("🔍 PerformanceAnalyzer aspect is ACTIVE!");
//        System.out.println("   Class: " + this.getClass().getName());
//    }

    @Around("execution(* com.llm.ai..controller..*.*(..)) || " +
            "execution(* com.llm.ai..service..*.*(..)) || " +
            "execution(* com.llm.ai..repository..*.*(..)) || " +
            "execution(* com.llm.ai..model..*.*(..)) || ")
    public Object measurePerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        long memoryBefore = getUsedMemory();

        try {
            return joinPoint.proceed();
        } catch (Throwable e) {
            long duration = System.currentTimeMillis() - startTime;
            long memoryUsed = getUsedMemory() - memoryBefore;

            String key = generateKey(joinPoint, e);
            recordError(key, duration, memoryUsed, e);

//            printPerformanceImpact(joinPoint, duration, memoryUsed, e);

            throw e;
        }
    }

    private String generateKey(ProceedingJoinPoint joinPoint, Throwable e) {
        return joinPoint.getSignature().getName() + ":" + e.getClass().getSimpleName();
    }

    public void recordError(String key, long duration, long memoryUsed, Throwable e) {
        errorMetrics.compute(key, (k, v) -> {
            if (v == null) return new ErrorMetric(duration, memoryUsed);
            v.addOccurrence(duration, memoryUsed);
            return v;
        });
    }

    private void printPerformanceImpact(ProceedingJoinPoint joinPoint, long duration, long memoryUsed, Throwable e) {
        System.out.println("\n📊 PERFORMANCE IMPACT:");
        System.out.println("   Method: " + joinPoint.getSignature().toShortString());
        System.out.println("   Response Time: " + duration + " ms");
        System.out.println("   Memory Used: " + (memoryUsed / 1024) + " KB");
        System.out.println("   Error: " + e.getClass().getSimpleName());
        System.out.println("\n");
    }

    private long getUsedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    public Map<String, ErrorMetric> getErrorMetrics() {
        return new HashMap<>(errorMetrics);
    }

    public void clearMetrics() {
        errorMetrics.clear();
        System.out.println("🧹 Performance metrics cleared");
    }

    public static class ErrorMetric {
        private int occurrences;
        private long totalDuration;
        private long maxDuration;
        private long totalMemory;
        private long maxMemory;

        public ErrorMetric(long duration, long memory) {
            this.occurrences = 1;
            this.totalDuration = duration;
            this.maxDuration = duration;
            this.totalMemory = memory;
            this.maxMemory = memory;
        }

        public void addOccurrence(long duration, long memory) {
            this.occurrences++;
            this.totalDuration += duration;
            this.maxDuration = Math.max(this.maxDuration, duration);
            this.totalMemory += memory;
            this.maxMemory = Math.max(this.maxMemory, memory);
        }

        public int getOccurrences() { return occurrences; }
        public long getTotalDuration() { return totalDuration; }
        public long getTotalMemory() { return totalMemory; }
        public long getAvgDuration() { return occurrences > 0 ? totalDuration / occurrences : 0;}
        public long getMaxDuration() { return maxDuration; }
        public long getAvgMemory() { return occurrences> 0 ? totalMemory / occurrences : 0;}
        public long getMaxMemory() { return maxMemory; }
    }
}
