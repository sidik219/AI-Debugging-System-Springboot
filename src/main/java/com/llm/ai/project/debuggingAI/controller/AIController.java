package com.llm.ai.project.debuggingAI.controller;

import com.llm.ai.core.common.UnitTestGenerator;
import com.llm.ai.core.filter.RateLimitFilter;
import com.llm.ai.project.debuggingAI.model.ErrorContext;
import com.llm.ai.project.debuggingAI.service.ErrorExtractorService;
import com.llm.ai.project.debuggingAI.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/debug")
public class AIController {

    @Autowired
    private ErrorExtractorService errorExtractor;

    @Autowired
    private UnitTestGenerator unitTestGenerator;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private RateLimitFilter rateLimitFilter;

    @Value("${debug.rate-limit.enabled:false}")
    private boolean rateLimitEnabled;

    @Value("${debug.rate-limit.requests:10}")
    private int maxRequests;

    @Value("${debug.rate-limit.seconds:60}")
    private int timeWindowSeconds;

    @Value("${debug.auth.enabled:false}")
    private boolean authEnabled;

    @Value("${debug.auth.token:}")
    private String authToken;

    // TODO: ==================== Default Test ====================

    @GetMapping("/test-error")
    public String testError() {
        String nullString = null;
        return nullString.toUpperCase();
    }

    @GetMapping("/test-divide")
    public int testDivide() {
        return 10 / 0;
    }

    @GetMapping("/test-array")
    public int testArray() {
        int[] numbers = {1, 2, 3};
        return numbers[5];
    }

    @GetMapping("/test-success")
    public String testSuccess() {
        return "Hello World!";
    }

    // TODO: ==================== Unit Test Generator ====================

    @GetMapping("/generate-test")
    public String generateTest() {
        try {
            throw new ArithmeticException("Test division by zero");
        } catch (Exception e) {
            ErrorContext context = errorExtractor.extractErrorContext(e);
            return unitTestGenerator.generateUnitTest(context);
        }
    }

    @GetMapping("/generate-controller-tests")
    public String generateAllTests() {
        String controllerClassName = "com.llm.ai.project.debuggingAI.controller.AIController";
        return unitTestGenerator.generateAllUnitTests(controllerClassName);
    }

    @GetMapping("/generate-service-tests")
    public String generateServiceTest() {
        String serviceClassName = "com.llm.ai.project.debuggingAI.service.AIDebugService";
        return unitTestGenerator.generateAllUnitTests(serviceClassName);
    }

    @GetMapping("/generate-repository-tests")
    public String generateRepositoryTests(@RequestParam(required = false) String className) {
        if (className == null || className.isEmpty()) {
            className = "com.llm.ai.project.debuggingAI.repository.DebugRepository";
        }
        return unitTestGenerator.generateAllUnitTests(className);
    }

    @GetMapping("/generate-model-tests")
    public String generateModelTest() {
        String modelClassName = "com.llm.ai.project.debuggingAI.model.ErrorContext";
        return unitTestGenerator.generateAllUnitTests(modelClassName);
    }

    // TODO: ========== NOTIFICATION ADMIN ==========

    @GetMapping("/notification-status")
    public Map<String, Object> notificationStatus() {
        return notificationService.getRateLimitStatus();
    }

    @GetMapping("/clear-rate-limit")
    public String clearRateLimit() {
        notificationService.clearRateLimitCache();
        return "✅ Rate limit cache cleared";
    }

    // TODO: ========== RATE LIMIT FILTER ADMIN ==========

    @GetMapping("/rate-limit-status")
    public Map<String, Object> rateLimitStatus() {
        return Map.of(
                "enabled", rateLimitEnabled,
                "maxRequests", maxRequests,
                "timeWindowSeconds", timeWindowSeconds,
                "activeClients", rateLimitFilter.getCacheSize()
        );
    }

    @GetMapping("/clear-rate-limit")
    public String clearRateLimit(@RequestHeader(value = "X-Debug-Token", required = false) String token) {
        // Cek token jika auth enabled
        if (authEnabled && (authToken == null || !authToken.equals(token))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        rateLimitFilter.clearCache();
        return "✅ Rate limit cache cleared";
    }

    // TODO: ==================== Ngawur Test ====================

    @GetMapping("/kontol")
    public String obj(String key) {
        Map<Integer, String> mmk = new HashMap<>();
        mmk.put(0,"knjt");

        Integer kenjut = Integer.parseInt(key);

        return mmk.get(kenjut);
    }
}
