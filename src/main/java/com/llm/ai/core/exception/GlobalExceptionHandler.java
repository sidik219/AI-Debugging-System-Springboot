package com.llm.ai.core.exception;

import com.llm.ai.core.aspect.PerformanceAnalyzer;
import com.llm.ai.project.debuggingAI.admin.service.NotificationService;
import com.llm.ai.project.debuggingAI.common.service.AIDebugService;
import com.llm.ai.project.debuggingAI.common.service.ClipboardService;
import com.llm.ai.project.debuggingAI.common.service.ErrorExtractorService;
import com.llm.ai.project.debuggingAI.common.service.ErrorHistoryService;
import com.llm.ai.project.debuggingAI.dashboard.controller.MonitoringController;
import com.llm.ai.project.debuggingAI.common.model.AIDebugResponse;
import com.llm.ai.project.debuggingAI.common.model.ErrorContext;
import com.llm.ai.project.debuggingAI.common.util.ConsoleColors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @Value("${debug.ai.provider:groq}")
    private String provider;

    @Value("${debug.ai.notify:true}")
    private boolean autoNotify;

    @Value("${debug.mode:development}")
    private String mode;

    @Autowired
    private ErrorExtractorService errorExtractor;

    @Autowired
    private AIDebugService aiDebugService;

    @Autowired
    private ErrorHistoryService historyService;

    @Autowired
    private ClipboardService clipboardService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private MonitoringController monitoringController;

    @Autowired
    private PerformanceAnalyzer performanceAnalyzer;

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleAllExceptions(Exception ex, WebRequest request) {
        long startTime = System.currentTimeMillis();
        long memoryBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        // Extract error context
        ErrorContext context = errorExtractor.extractErrorContext(ex);

        // Get AI analysis
        AIDebugResponse aiResponse = aiDebugService.analyzeError(context);

        // Record performance
        printRecordPerformance(context, startTime, memoryBefore, ex);

        // Print debug info
        if ("production".equals(mode)) {
            printProductionError(context, aiResponse);
        } else {
            printDebugInfoToConsole(context, aiResponse);
        }

        // Save history
        historyService.saveErrorHistory(context, aiResponse, provider);

        // Copy to clipboard
        clipboardService.copySolutionToClipboard(aiResponse);

        // Summary Report
        notificationService.recordErrorForSummary(context);

        // Send notification
        if (autoNotify) {
            notificationService.sendErrorNotification(context, aiResponse, provider);
        }

        // Dashboard
        monitoringController.broadcastNewError(context, aiResponse.getAnalysis(), aiResponse.getSuggestedFix());

        // Development: tambah detail exception
        Map<String, Object> errorResponse = new LinkedHashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        errorResponse.put("error", "Internal Server Error");
        errorResponse.put("path", request.getDescription(false).replace("uri=", ""));

        if (!"production".equals(mode)) {
            errorResponse.put("exception", ex.getClass().getName());
            errorResponse.put("message", ex.getMessage());
        }

        return new ResponseEntity<>(
                "Error occurred - Check console for AI debugging assistance",
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    private void printProductionError(ErrorContext context, AIDebugResponse aiResponse) {
        System.out.println("\n" + ConsoleColors.RED_BOLD + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" + ConsoleColors.RESET);
        System.out.println(ConsoleColors.RED_BOLD + "🚨 " +
                context.getExceptionType().substring(context.getExceptionType().lastIndexOf('.') + 1) +
                ConsoleColors.RESET + " di " + ConsoleColors.YELLOW + context.getMethodName() + "()" + ConsoleColors.RESET);
        System.out.println(ConsoleColors.RED_BOLD + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" + ConsoleColors.RESET);
        System.out.println(ConsoleColors.YELLOW + "📌 " + context.getMessage() + ConsoleColors.RESET);
        System.out.println("\n" + ConsoleColors.GREEN + "💡 Perbaikan:" + ConsoleColors.RESET);
        System.out.println("   " + aiResponse.getSuggestedFix().replace("\n", "\n   "));
    }

    private void printDebugInfoToConsole(ErrorContext context, AIDebugResponse aiResponse) {
        System.out.println("\n" + ConsoleColors.RED_BOLD + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" + ConsoleColors.RESET);
        System.out.println(ConsoleColors.RED_BOLD + "🚨 " + context.getExceptionType().substring(context.getExceptionType().lastIndexOf('.') + 1) +
                ConsoleColors.RESET + " di " + ConsoleColors.YELLOW + context.getMethodName() + "()" + ConsoleColors.RESET);
        System.out.println(ConsoleColors.RED_BOLD + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" + ConsoleColors.RESET);

        // Error Info (ringkas)
        System.out.println(ConsoleColors.YELLOW + "📌 " + context.getMessage() + ConsoleColors.RESET);
        System.out.println(ConsoleColors.BLUE + "📍 " + context.getClassName() + "." + context.getMethodName() + "() : line " + context.getLineNumber() + ConsoleColors.RESET);

        // Source Code (hanya baris error + 2 baris konteks)
        System.out.println("\n" + ConsoleColors.CYAN + "📝 Kode:" + ConsoleColors.RESET);
        printMinimalSourceCode(context);

        // AI Analysis (ringkas)
        System.out.println("\n" + ConsoleColors.PURPLE + "🤖 AI (" + provider.toUpperCase() + "):" + ConsoleColors.RESET);
        System.out.println("   " + aiResponse.getAnalysis().replace("\n", "\n   "));

        // Suggested Fix
        System.out.println("\n" + ConsoleColors.GREEN + "💡 Perbaikan:" + ConsoleColors.RESET);
        System.out.println("   " + aiResponse.getSuggestedFix().replace("\n", "\n   "));

        // Code Example (jika ada)
        if (aiResponse.getCodeExample() != null && !aiResponse.getCodeExample().isEmpty()) {
            System.out.println("\n" + ConsoleColors.CYAN + "📋 Contoh:" + ConsoleColors.RESET);
            System.out.println("   " + aiResponse.getCodeExample().replace("\n", "\n   "));
        }

        // Confidence
        String confidence = aiResponse.getConfidence();
        String confidenceIcon = confidence.equals("HIGH") ? "🟢" : (confidence.equals("MEDIUM") ? "🟡" : "🔴");
        System.out.println("\n" + confidenceIcon + " Keyakinan: " + confidence);

        System.out.println(ConsoleColors.RED_BOLD + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" + ConsoleColors.RESET);
    }

    private void printMinimalSourceCode(ErrorContext context) {
        String sourceCode = context.getSourceCode();
        if (sourceCode == null || sourceCode.isEmpty()) return;

        String[] lines = sourceCode.split("\n");
        int errorLine = context.getLineNumber();

        // Cari baris yang ada marker 👉
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].contains("👉")) {
                // Print 1 baris sebelum (jika ada)
                if (i > 0) {
                    System.out.println("   " + lines[i-1]);
                }
                // Print baris error
                System.out.println(ConsoleColors.RED + "   " + lines[i] + ConsoleColors.RESET);
                // Print 1 baris setelah (jika ada)
                if (i < lines.length - 1) {
                    System.out.println("   " + lines[i+1]);
                }
                break;
            }
        }
    }

    private void printRecordPerformance(ErrorContext context, long startTime, long memoryBefore, Throwable ex) {
        long duration = System.currentTimeMillis() - startTime;
        long memoryUsed = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) - memoryBefore;

        String key = context.getMethodName() + ":" + ex.getClass().getSimpleName();
        performanceAnalyzer.recordError(key, duration, memoryUsed, ex);

        System.out.println("\n📊 PERFORMANCE IMPACT:");
        System.out.println("   Method: " + context.getMethodName() + "()");
        System.out.println("   Response Time: " + duration + " ms");
        System.out.println("   Memory Used: " + (memoryUsed / 1024) + " KB");
        System.out.println("   Error: " + ex.getClass().getSimpleName());
    }
}
