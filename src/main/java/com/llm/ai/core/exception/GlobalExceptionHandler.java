package com.llm.ai.core.exception;

import com.llm.ai.project.debuggingAI.model.AIDebugResponse;
import com.llm.ai.project.debuggingAI.model.ErrorContext;
import com.llm.ai.project.debuggingAI.service.*;
import com.llm.ai.project.debuggingAI.util.ConsoleColors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

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

    @Value("${debug.ai.provider:groq}")
    private String provider;

    @Value("${debug.ai.notify:true}")
    private boolean autoNotify;

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleAllExceptions(Exception ex) {
        System.out.println("\n" + ConsoleColors.RED_BG + "🚨 Exception tertangkap: " + ex.getClass().getSimpleName() + ConsoleColors.RESET);

        // Extract error context
        ErrorContext context = errorExtractor.extractErrorContext(ex);

        // Get AI analysis
        AIDebugResponse aiResponse = aiDebugService.analyzeError(context);

        // Print debug info
        printDebugInfoToConsole(context, aiResponse);

        // Save history
        historyService.saveErrorHistory(context, aiResponse, provider);

        // Copy to clipboard
        clipboardService.copySolutionToClipboard(aiResponse);

        // Send notification
        System.out.println(ConsoleColors.CYAN + "📢 autoNotify = " + autoNotify + ConsoleColors.RESET);
        if (autoNotify) {
            System.out.println(ConsoleColors.CYAN + "📢 Memanggil notificationService..." + ConsoleColors.RESET);
            notificationService.sendErrorNotification(context, aiResponse, provider);
        } else {
            System.out.println(ConsoleColors.YELLOW + "🔕 Auto-notify dinonaktifkan (debug.auto.notify=false)" + ConsoleColors.RESET);
        }

        return new ResponseEntity<>(
                "Error occurred - Check console for AI debugging assistance",
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    private void printDebugInfoToConsole(ErrorContext context, AIDebugResponse aiResponse) {
        System.out.println("\n" + ConsoleColors.CYAN_BOLD + "=".repeat(80) + ConsoleColors.RESET);
        System.out.println(ConsoleColors.PURPLE_BOLD + "🔍 ASISTEN DEBUG AI - ANALISIS ERROR" + ConsoleColors.RESET);
        System.out.println(ConsoleColors.CYAN_BOLD + "=".repeat(80) + ConsoleColors.RESET);

        System.out.println("\n" + ConsoleColors.RED_BOLD + "❌ ERROR TERDETEKSI:" + ConsoleColors.RESET);
        System.out.printf("   " + ConsoleColors.RED + "Tipe: %s" + ConsoleColors.RESET + "%n", context.getExceptionType());
        System.out.printf("   " + ConsoleColors.YELLOW + "Pesan: %s" + ConsoleColors.RESET + "%n", context.getMessage());
        System.out.printf("   " + ConsoleColors.BLUE + "Lokasi: %s.%s() pada baris %d" + ConsoleColors.RESET + "%n",
                context.getClassName(), context.getMethodName(), context.getLineNumber());

        System.out.println("\n" + ConsoleColors.GREEN_BOLD + "📄 KONTEKS KODE SUMBER:" + ConsoleColors.RESET);
        System.out.println(ConsoleColors.GREEN + "-".repeat(40) + ConsoleColors.RESET);
        System.out.println(context.getSourceCode());

        System.out.println("\n" + ConsoleColors.PURPLE_BOLD + "🤖 ANALISIS AI (" + provider.toUpperCase() + "):" + ConsoleColors.RESET);
        System.out.println(ConsoleColors.PURPLE + "-".repeat(40) + ConsoleColors.RESET);
        System.out.println(aiResponse.getAnalysis());

        System.out.println("\n" + ConsoleColors.YELLOW_BOLD + "💡 SARAN PERBAIKAN:" + ConsoleColors.RESET);
        System.out.println(ConsoleColors.YELLOW + "-".repeat(40) + ConsoleColors.RESET);
        System.out.println(aiResponse.getSuggestedFix());

        if (aiResponse.getCodeExample() != null && !aiResponse.getCodeExample().isEmpty()) {
            System.out.println("\n" + ConsoleColors.CYAN_BOLD + "📝 CONTOH KODE:" + ConsoleColors.RESET);
            System.out.println(ConsoleColors.CYAN + "-".repeat(40) + ConsoleColors.RESET);
            System.out.println(aiResponse.getCodeExample());
        }

        String confidenceIcon = switch(aiResponse.getConfidence()) {
            case "HIGH" -> ConsoleColors.GREEN + "🟢 TINGGI" + ConsoleColors.RESET;
            case "MEDIUM" -> ConsoleColors.YELLOW + "🟡 SEDANG" + ConsoleColors.RESET;
            default -> ConsoleColors.RED + "🔴 RENDAH" + ConsoleColors.RESET;
        };
        System.out.printf("%n📊 TINGKAT KEYAKINAN: %s%n", confidenceIcon);

        System.out.println("\n" + ConsoleColors.CYAN_BOLD + "=".repeat(80) + ConsoleColors.RESET);
        System.out.println(ConsoleColors.GREEN_BOLD + "💪 Coba saran perbaikan dan jalankan ulang!" + ConsoleColors.RESET);
        System.out.println(ConsoleColors.CYAN_BOLD + "=".repeat(80) + ConsoleColors.RESET + "\n");
    }
}
