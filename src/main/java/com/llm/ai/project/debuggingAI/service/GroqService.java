package com.llm.ai.project.debuggingAI.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
public class GroqService {

    @Value("${groq.api.key:}")
    private String apiKey;

    @Value("${groq.model:llama-3.3-70b-versatile}")
    private String model;

    private final WebClient webClient;

    public GroqService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl("https://api.groq.com/openai/v1")
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @PostConstruct
    public void init() {
        boolean hasKey = apiKey != null && !apiKey.isEmpty() && !apiKey.startsWith("${");
        System.out.println("🔑 Groq API Key configured: " + (hasKey ? "✅ YES" : "❌ NO"));
    }

    public Mono<String> analyzeError(String prompt) {
        if (apiKey == null || apiKey.isEmpty() || apiKey.startsWith("${")) {
            System.err.println("❌ Groq API key is not configured!");
            return Mono.empty();
        }

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content",
                                "You are an expert Java Spring Boot developer specializing in debugging. " +
                                        "Provide concise, actionable solutions to coding errors."),
                        Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.1,
                "max_tokens", 1000
        );

        return webClient.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    var choices = (List<Map<String, Object>>) response.get("choices");
                    if (choices != null && !choices.isEmpty()) {
                        var message = (Map<String, Object>) choices.get(0).get("message");
                        return (String) message.get("content");
                    }
                    return null;
                })
                .onErrorResume(e -> {
                    System.err.println("❌ Error calling Groq API: " + e.getMessage());
                    return Mono.empty();
                });
    }
}