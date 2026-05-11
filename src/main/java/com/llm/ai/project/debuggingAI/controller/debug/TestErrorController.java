package com.llm.ai.project.debuggingAI.controller.debug;

import com.llm.ai.core.common.UnitTestGenerator;
import com.llm.ai.core.component.DebugSession;
import com.llm.ai.core.component.RateLimitFilter;
import com.llm.ai.project.debuggingAI.model.AIDebugResponse;
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
public class TestErrorController {

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

    // TODO: ==================== Ngawur Test ====================

    @GetMapping("/bebas")
    public String obj(String key) {
        Map<Integer, String> mmk = new HashMap<>();
        mmk.put(0,"knjt");

        Integer kenjut = Integer.parseInt(key);

        return mmk.get(kenjut);
    }
}
