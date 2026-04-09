package com.llm.ai.core.component;

import com.llm.ai.project.debuggingAI.model.AIDebugResponse;
import com.llm.ai.project.debuggingAI.model.ErrorContext;
import com.llm.ai.project.debuggingAI.util.ConsoleColors;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DebugSession {

    private final Map<String, List<FixAttempt>> attemptHistory = new ConcurrentHashMap<>();
    private final Map<String, String> successfulFixes = new ConcurrentHashMap<>();
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

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
