package com.llm.ai.project.debuggingAI.controller.debug;

import com.llm.ai.core.common.UnitTestGenerator;
import com.llm.ai.project.debuggingAI.model.ErrorContext;
import com.llm.ai.project.debuggingAI.service.ErrorExtractorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/generator")
public class UnitTestGeneratorController {

    @Autowired
    private ErrorExtractorService errorExtractor;

    @Autowired
    private UnitTestGenerator unitTestGenerator;

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
        String controllerClassName = "com.llm.ai.project.debuggingAI.controller.ecosystem.EcosystemTestController";
        return unitTestGenerator.generateAllUnitTests(controllerClassName);
    }

    @GetMapping("/generate-service-tests")
    public String generateServiceTest() {
        String serviceClassName = "com.llm.ai.project.debuggingAI.service.TestService";
        return unitTestGenerator.generateAllUnitTests(serviceClassName);
    }

    @GetMapping("/generate-repository-tests")
    public String generateRepositoryTests(@RequestParam(required = false) String className) {
        if (className == null || className.isEmpty()) {
            className = "com.llm.ai.project.debuggingAI.repository.TestRepository";
        }
        return unitTestGenerator.generateAllUnitTests(className);
    }

    @GetMapping("/generate-model-tests")
    public String generateModelTest() {
        String modelClassName = "com.llm.ai.project.debuggingAI.model.TestEntity";
        return unitTestGenerator.generateAllUnitTests(modelClassName);
    }
}
