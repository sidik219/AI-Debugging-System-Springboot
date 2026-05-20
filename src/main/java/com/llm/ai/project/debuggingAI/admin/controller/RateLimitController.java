package com.llm.ai.project.debuggingAI.admin.controller;

import com.llm.ai.core.component.RateLimitFilter;
import com.llm.ai.project.debuggingAI.admin.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/rate-limit")
public class RateLimitController {

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

        // Clear semua cache
        notificationService.clearRateLimitCache();
        rateLimitFilter.clearCache();

        return "✅ All rate limit caches cleared";
    }
}
