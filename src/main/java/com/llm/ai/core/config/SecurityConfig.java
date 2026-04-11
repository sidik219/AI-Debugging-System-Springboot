package com.llm.ai.core.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${debug.auth.enabled:false}")
    private boolean authEnabled;

    @Value("${debug.auth.token:}")
    private String authToken;

    @Bean
    public UserDetailsService userDetailService() {
        return new InMemoryUserDetailsManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        if (!authEnabled || authToken == null || authToken.isEmpty()) {
            System.out.println("🔓 Security: AUTH DISABLED - Semua endpoint public");
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            http.csrf(csrf -> csrf.disable());

            return http.build();
        }

        System.out.println("🔒 Security: AUTH ENABLED - /api/debug/* butuh token");
        http
                .authorizeHttpRequests(
                        auth -> auth
                                .requestMatchers("/api/debug/**")
                                .authenticated()
                                .anyRequest()
                                .permitAll()
                )
                .addFilterBefore(new TokenAuthFilter(authToken), UsernamePasswordAuthenticationFilter.class)
                .sessionManagement(
                        session -> session
                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .csrf(csrf -> csrf.disable());

        return http.build();
    }

    private static class TokenAuthFilter extends OncePerRequestFilter {

        private final String validToken;

        public TokenAuthFilter(String validToken) {
            this.validToken = validToken;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request,
                                        HttpServletResponse response,
                                        FilterChain chain) throws IOException, ServletException {

            String path = request.getRequestURI();

            // Hanya cek untuk endpoint /api/debug/
            if (path.startsWith("/api/debug/")) {
                String token = request.getHeader("X-Debug-Token");

                if (validToken.equals(token)) {
                    // Token valid → set authentication
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken("debug", null, Collections.emptyList());
                    SecurityContextHolder.getContext().setAuthentication(auth);
                } else {
                    // Token invalid → 401
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\": \"Invalid or missing X-Debug-Token header\"}");
                    return;
                }
            }

            chain.doFilter(request, response);
        }
    }
}
