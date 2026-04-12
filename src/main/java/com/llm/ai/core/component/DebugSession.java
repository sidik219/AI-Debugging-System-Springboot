package com.llm.ai.core.component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.llm.ai.project.debuggingAI.model.AIDebugResponse;
import com.llm.ai.project.debuggingAI.model.ErrorContext;
import com.llm.ai.project.debuggingAI.util.ConsoleColors;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DebugSession {

    @Value("${debug.session.persist:true}")
    private boolean persistSession;

    @Value("${debug.session.path:./error-logs/sessions}")
    private String sessionPath;

    private final Map<String, List<FixAttempt>> attemptHistory = new ConcurrentHashMap<>();
    private final Map<String, String> successfulFixes = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private int loadedFixesCount = 0;

    @PostConstruct
    public void loadFromFile() {
        if (!persistSession) return;

        try {
            File file = new File(sessionPath + "/successful-fixes.json");
            if (file.exists()) {
                Map<String, String> loadedFixes = objectMapper.readValue(
                        file,
                        new TypeReference<Map<String, String>>(){}
                );

                successfulFixes.putAll(loadedFixes);
                loadedFixesCount = loadedFixes.size();
            }
        } catch (Exception e) {
            System.err.println("⚠️ Failed to load session: " + e.getMessage());
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void printLoadStatus() {
        if (persistSession && loadedFixesCount > 0) {
            System.out.println(ConsoleColors.GREEN + "📂 Session loaded: " + loadedFixesCount + " cached fixes" + ConsoleColors.RESET);
        }
    }

    @PreDestroy
    public void saveToFile() {
        if (!persistSession) return;

        try {
            File dir = new File(sessionPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(new File(sessionPath + "/successful-fixes.json"), successfulFixes);

            System.out.println(ConsoleColors.GREEN + "💾 Session saved: " + successfulFixes.size() + " fixes" + ConsoleColors.RESET);
        } catch (IOException e) {
            System.err.println("❌ Failed to save session: " + e.getMessage());
        }
    }

    public void recordAttempt(ErrorContext context, AIDebugResponse response) {
        String signature = generateSignature(context);

        FixAttempt attempt = new FixAttempt(
                LocalDateTime.now(),
                response.getSuggestedFix(),
                response.getCodeExample()
        );

        attemptHistory.computeIfAbsent(signature, k -> new ArrayList<>()).add(attempt);

        System.out.println(
                ConsoleColors.CYAN +
                "📝 Attempt #" + attemptHistory.get(signature).size() +
                " recorded for: " + context.getMethodName() +
                "()" +
                ConsoleColors.RESET
        );
    }

    public void recordSuccess(ErrorContext context, AIDebugResponse response) {
        String signature = generateSignature(context);
        successfulFixes.put(signature, response.getSuggestedFix());
        System.out.println(ConsoleColors.GREEN + "✅ Successful fix recorded!" + ConsoleColors.RESET);
    }

    public String getSessionContext(ErrorContext context) {
        String signature = generateSignature(context);
        List<FixAttempt> attempts = attemptHistory.get(signature);

        if (attempts == null || attempts.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\n=== SESI DEBUGGING SEBELUMNYA ===\n");
        sb.append("Percobaan fix yang SUDAH GAGAL:\n");

        for (int i = 0; i < attempts.size(); i++) {
            FixAttempt attempt = attempts.get(i);
            sb.append(String.format("\n[Attempt #%d - %s]\n", i + 1, attempt.time.format(TIME_FORMAT)));
            sb.append("Solusi: ").append(attempt.solution).append("\n");
        }

        sb.append("\n⚠️ JANGAN ULANGI SOLUSI DI ATAS. Berikan solusi yang BERBEDA.\n");
        sb.append("================================\n");

        return sb.toString();
    }

    public String getCachedSolution(ErrorContext context) {
        String signature = generateSignature(context);
        return successfulFixes.get(signature);
    }

    public void clearSession(ErrorContext context) {
        String signature = generateSignature(context);
        attemptHistory.remove(signature);
        System.out.println(ConsoleColors.YELLOW + "🔄 Session cleared for: " + context.getMethodName() + "()" + ConsoleColors.RESET);
    }

    public int getAttemptCount(ErrorContext context) {
        String signature = generateSignature(context);
        List<FixAttempt> attempts = attemptHistory.get(signature);
        return attempts != null ? attempts.size() : 0;
    }

    private String generateSignature(ErrorContext context) {
        return context.getExceptionType() + ":" +
                context.getClassName() + ":" +
                context.getMethodName() + ":" +
                context.getLineNumber();
    }

    public void printSummary() {
        System.out.println("\n" + ConsoleColors.PURPLE_BOLD + "📊 DEBUG SESSION SUMMARY" + ConsoleColors.RESET);
        System.out.println(ConsoleColors.PURPLE + "=".repeat(50) + ConsoleColors.RESET);
        System.out.println("Total errors tracked: " + attemptHistory.size());
        System.out.println("Successful fixes cached: " + successfulFixes.size());

        if (!attemptHistory.isEmpty()) {
            System.out.println("\nActive sessions:");
            attemptHistory.forEach((sig, attempts) -> {
                String shortSig = sig.substring(sig.lastIndexOf('.') + 1);
                System.out.printf("  • %s (%d attempts)%n", shortSig, attempts.size());
            });
        }
        System.out.println();
    }

    public void clearAll() {
        attemptHistory.clear();
        successfulFixes.clear();

        if (persistSession) {
            try {
                new File(sessionPath + "/successful-fixes.json").delete();
                new File(sessionPath + "/attempts.json").delete();
            } catch (Exception e) {}
        }

        System.out.println(ConsoleColors.YELLOW + "🧹 Session cleared" + ConsoleColors.RESET);
    }

    public Map<String, Object> getStats() {
        return Map.of(
                "attemptHistorySize", attemptHistory.size(),
                "successfulFixesSize", successfulFixes.size(),
                "persistEnabled", persistSession,
                "sessionPath", sessionPath
        );
    }

    private static class FixAttempt {
        final LocalDateTime time;
        final String solution;
        final String code;

        FixAttempt(LocalDateTime time, String solution, String code) {
            this.time = time;
            this.solution = solution;
            this.code = code;
        }
    }
}
