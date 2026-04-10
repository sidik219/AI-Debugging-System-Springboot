package com.llm.ai.project.debuggingAI.controller;

import com.llm.ai.core.common.UnitTestGenerator;
import com.llm.ai.project.debuggingAI.model.ErrorContext;
import com.llm.ai.project.debuggingAI.service.ErrorExtractorService;
import com.llm.ai.project.debuggingAI.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

    // ========== NOTIFICATION ADMIN ==========

    @GetMapping("/notification-status")
    public Map<String, Object> notificationStatus() {
        return notificationService.getRateLimitStatus();
    }

    @GetMapping("/clear-rate-limit")
    public String clearRateLimit() {
        notificationService.clearRateLimitCache();
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
