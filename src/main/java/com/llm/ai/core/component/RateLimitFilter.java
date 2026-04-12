package com.llm.ai.core.component;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Order(1)
public class RateLimitFilter implements Filter {

    @Value("${debug.rate-limit.enabled:false}")
    private boolean rateLimitEnabled;

    @Value("${debug.rate-limit.requests:10}")
    private int maxRequests;

    @Value("${debug.rate-limit.seconds:60}")
    private int timeWindowSeconds;

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest)  request;
        HttpServletResponse httpResponse = (HttpServletResponse)  response;

        String path = httpRequest.getRequestURI();

        if (!rateLimitEnabled || !path.startsWith("/api/debug/")) {
            chain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIp(httpRequest);

        Bucket bucket = buckets.computeIfAbsent(clientIp, k -> creatNewBucket());

        if (bucket.tryConsume(1)) {
            long tokensLeft = bucket.getAvailableTokens();
            httpResponse.setHeader("X-Rate-Limit-Remaining", String.valueOf(tokensLeft));
            chain.doFilter(request, response);
        } else {
            httpResponse.setStatus(429);
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write(String.format(
                    "{\"error\": \"Rate limit exceeded. Max %d requests per %d seconds.\"}",
                    maxRequests, timeWindowSeconds
            ));
        }
    }

    private Bucket creatNewBucket() {
        Bandwidth limit = Bandwidth.classic(
                maxRequests,
                Refill.greedy(maxRequests, Duration.ofSeconds(timeWindowSeconds))
        );

        return Bucket.builder().addLimit(limit).build();
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    public void clearCache() {
        buckets.clear();
        System.out.println("🧹 Rate limit cache cleared");
    }

    public int getCacheSize() {
        return buckets.size();
    }
}
