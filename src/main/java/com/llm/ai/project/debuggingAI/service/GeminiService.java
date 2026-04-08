package com.llm.ai.project.debuggingAI.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.model:gemini-2.0-flash-lite}")
    private String model;

    private final RestTemplate restTemplate;

    public GeminiService() {
        this.restTemplate = new RestTemplate();
    }

    @PostConstruct
    public void init() {
        boolean hasKey = apiKey != null && !apiKey.isEmpty() && !apiKey.equals("${GEMINI_API_KEY:}");
        System.out.println("🔑 Gemini API Key configured: " + (hasKey ? "✅ YES" : "❌ NO"));
    }

    public String analyzeError(String prompt) {
        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("❌ Gemini API key is not configured!");
            return null;
        }

        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                + model + ":generateContent?key=" + apiKey;

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)
                        ))
                ),
                "generationConfig", Map.of(
                        "temperature", 0.1,
                        "maxOutputTokens", 1000
                )
        );

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            System.out.println("🤖 Calling Gemini API...");

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            if (response.getBody() != null) {
                List<Map<String, Object>> candidates =
                        (List<Map<String, Object>>) response.getBody().get("candidates");

                if (candidates != null && !candidates.isEmpty()) {
                    Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                    List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");

                    if (parts != null && !parts.isEmpty()) {
                        System.out.println("✅ Gemini API response received");
                        return (String) parts.get(0).get("text");
                    }
                }
            }

            return null;

        } catch (Exception e) {
            System.err.println("❌ Error calling Gemini API: " + e.getMessage());
            return null;
        }
    }
}