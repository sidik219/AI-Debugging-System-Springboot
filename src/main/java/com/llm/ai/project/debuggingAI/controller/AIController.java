package com.llm.ai.project.debuggingAI.controller;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/debug")
public class AIController {

    // TODO: Default Test
    @GetMapping("/test-error")
    public String testError() {
        String nullString = null;
        return nullString.toUpperCase(); // Ini akan trigger NPE
    }

    @GetMapping("/test-divide")
    public int testDivide() {
        return 10 / 0; // Ini akan trigger ArithmeticException
    }

    @GetMapping("/test-array")
    public int testArray() {
        int[] numbers = {1, 2, 3};
        return numbers[5]; // Ini akan trigger ArrayIndexOutOfBoundsException
    }

    @GetMapping("/test-success")
    public String testSuccess() {
        return "Hello World!";
    }

    // TODO: Test Ngawur
    @GetMapping("/kontol")
    public String obj(String key) {
        Map<Integer, String> mmk = new HashMap<>();
        mmk.put(0,"knjt");

        Integer kenjut = Integer.parseInt(key);

        return mmk.get(kenjut);
    }

    @GetMapping("/Presidensial")
    public String PriaSolo(
            @RequestParam String Jokowi,
            @RequestParam Integer Prabowo
    ) {

        if (Jokowi.equals("error")) {
            throw new RuntimeException("Error dari Jokowi");
        }

        if (Prabowo == 0) {
            int hasil = 10 / Prabowo;
        }

        int parsed = Integer.parseInt(Jokowi);

        return String.valueOf(parsed + Prabowo);
    }
}
