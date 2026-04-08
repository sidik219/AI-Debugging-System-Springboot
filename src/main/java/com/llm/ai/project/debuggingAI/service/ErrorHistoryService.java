package com.llm.ai.project.debuggingAI.service;

import com.llm.ai.project.debuggingAI.model.AIDebugResponse;
import com.llm.ai.project.debuggingAI.model.ErrorContext;
import com.llm.ai.project.debuggingAI.util.ConsoleColors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class ErrorHistoryService {

    @Value("${debug.history.enabled:true}")
    private boolean historyEnabled;

    @Value("${debug.history.path:./error-logs}")
    private String logPath;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    public void saveErrorHistory(ErrorContext context, AIDebugResponse response, String provider) {
        if (!historyEnabled) return;

        try {
            Path logDir = Paths.get(logPath);
            if (!Files.exists(logDir)) {
                Files.createDirectories(logDir);
            }

            String filename = "error-history" + LocalDateTime.now().format(DATE_FORMAT) + ".log";
            Path logFile = logDir.resolve(filename);

            String content = formatLogEntry(context, response, provider);
            Files.write(logFile, content.getBytes(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);

            System.out.println(ConsoleColors.CYAN + "📁 Error tersimpan di: " + logFile + ConsoleColors.RESET);
        } catch (IOException e) {
            System.err.println("❌ Gagal menyimpan history: " + e.getMessage());
        }
    }

    private String formatLogEntry(ErrorContext context, AIDebugResponse response, String provider) {
        LocalDateTime now = LocalDateTime.now();

        return String.format("""
            ========================================
            📅 Tanggal  : %s
            🕐 Waktu    : %s
            🤖 Provider : %s
            ----------------------------------------
            ❌ ERROR:
               Tipe     : %s
               Pesan    : %s
               Lokasi   : %s.%s() baris %d
            ----------------------------------------
            🔍 ANALISIS AI:
            %s
            ----------------------------------------
            💡 SOLUSI:
            %s
            ----------------------------------------
            📝 KODE PERBAIKAN:
            %s
            ----------------------------------------
            📊 KEYAKINAN: %s
            ========================================
            
            """,
                now.format(DATE_FORMAT),
                now.format(TIME_FORMAT),
                provider,
                context.getExceptionType(),
                context.getMessage(),
                context.getClassName(),
                context.getMethodName(),
                context.getLineNumber(),
                response.getAnalysis() != null ? response.getAnalysis() : "-",
                response.getSuggestedFix() != null ? response.getSuggestedFix() : "-",
                response.getCodeExample() != null ? response.getCodeExample() : "-",
                response.getConfidence() != null ? response.getConfidence() : "-"
        );
    }

    public void printSummary() {
        try {
            Path logDir = Paths.get(logPath);
            if (!Files.exists(logDir)) {
                System.out.println("📭 Belum ada history error.");
                return;
            }

            long fileCount = Files.list(logDir).filter(f -> f.toString().endsWith(".log")).count();

            System.out.println(ConsoleColors.CYAN + "📊 Total " + fileCount + " file log error tersimpan di: " + logDir.toAbsolutePath() + ConsoleColors.RESET);
        } catch (IOException e) {
            System.err.println("❌ Gagal membaca history: " + e.getMessage());
        }
    }
}
