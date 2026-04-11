package com.llm.ai.project.debuggingAI.service;

import com.llm.ai.project.debuggingAI.util.ConsoleColors;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
public class OpenAIService {

    @Value("${openai.api.key:}")
    private String apiKey;

    @Value("${openai.model:gpt-3.5-turbo}")
    private String model;

    private final WebClient webClient;

    public OpenAIService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @PostConstruct
    public void init() {
        boolean hasKey = apiKey != null && !apiKey.isEmpty() && !apiKey.startsWith("${");
        System.out.println(ConsoleColors.CYAN + "🔑 OpenAI API Key: " + (hasKey ? "✅ YES" : "❌ NOT SET") + ConsoleColors.RESET);
    }

    public Mono<String> analyzeError(String prompt) {
        if (apiKey == null || apiKey.isEmpty() || apiKey.startsWith("${")) {
            System.err.println("❌ OpenAI API key not configured!");
            return Mono.empty();
        }

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.1
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
                });
    }
}