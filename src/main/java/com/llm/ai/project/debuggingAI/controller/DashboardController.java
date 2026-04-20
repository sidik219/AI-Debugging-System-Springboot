package com.llm.ai.project.debuggingAI.controller;

import com.llm.ai.project.debuggingAI.model.ErrorContext;
import com.llm.ai.project.debuggingAI.service.ErrorHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("api/dashboard")
@CrossOrigin(origins = "*")
public class DashboardController {

    @Autowired
    private ErrorHistoryService historyService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public void broadcastNewError(ErrorContext context, String analysis, String fix) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("timestamp", context.getTimestamp().toString());
        payload.put("type", getSimpleExceptionName(context.getExceptionType()));
        payload.put("message", context.getMessage());
        payload.put("location", context.getClassName() + "." + context.getMethodName() + "()");
        payload.put("lineNumber", context.getLineNumber());
        payload.put("analysis", analysis != null ? analysis : "-");
        payload.put("fix", fix != null ? fix : "-");

        messagingTemplate.convertAndSend("/topic/errors", payload);
    }

    @GetMapping("/stats")
    public Map<String, Object> getStats() {
//        Map<String, Object> stats = new HashMap<>();
//        stats.put("totalToday", 47);
//        stats.put("fixedToday", 38);
//        stats.put("successRate", 81);
//        stats.put("avgResponseTime", "2.3s");
//
//        return stats;

        Map<String, Object> stats = new HashMap<>();

        try {
            Path logFile = getTodayLogFile();

            System.out.println("📊 Reading stats from: " + logFile.toAbsolutePath());

            if (!Files.exists(logFile)) {
                System.out.println("   ⚠️ File not found");
                stats.put("totalToday", 0);
                stats.put("fixedToday", 0);
                stats.put("successRate", 0);
                stats.put("avgResponseTime", "0s");
                return stats;
            }

            String content = Files.readString(logFile);

            // Hitung jumlah error (setiap "========================================")
            int totalErrors = countOccurrences(content, "========================================") / 2;

            // Hitung fixed (KEYAKINAN: HIGH atau SOLUSI ada)
            int fixedErrors = 0;
            String[] blocks = content.split("========================================\n");
            for (String block : blocks) {
                if (block.contains("KEYAKINAN: HIGH") ||
                        (block.contains("💡 SOLUSI:") && !block.contains("💡 SOLUSI:\n-"))) {
                    fixedErrors++;
                }
            }

            int successRate = totalErrors > 0 ? (fixedErrors * 100) / totalErrors : 0;

            stats.put("totalToday", totalErrors);
            stats.put("fixedToday", fixedErrors);
            stats.put("successRate", successRate);
            stats.put("avgResponseTime", "1.2s");

            System.out.println("   ✅ Total: " + totalErrors + ", Fixed: " + fixedErrors);

        } catch (IOException e) {
            System.err.println("❌ Failed to read stats: " + e.getMessage());
            stats.put("totalToday", 0);
            stats.put("fixedToday", 0);
            stats.put("successRate", 0);
            stats.put("avgResponseTime", "0s");
        }

        return stats;
    }

    @GetMapping("/trends")
    public Map<String, Object> getTrends() {
//        Map<String, Object> trends = new HashMap<>();
//
//        List<String> labels = Arrays.asList(
//                "00", "02", "04", "06", "08",
//                "10", "12", "14", "16", "18",
//                "20", "22"
//        );
//        List<Integer> data = Arrays.asList(
//                2, 1, 0, 3, 5,
//                8, 12, 15, 10,
//                7, 4, 2
//        );
//
//        trends.put("labels", labels);
//        trends.put("data", data);
//
//        return trends;

        Map<String, Object> trends = new HashMap<>();

        try {
            Path logFile = getTodayLogFile();

            if (!Files.exists(logFile)) {
                trends.put("labels", generateHourLabels());
                trends.put("data", new int[24]);
                return trends;
            }

            List<String> labels = new ArrayList<>();
            int[] hourlyData = new int[24];

            for (int i = 0; i < 24; i++) {
                labels.add(String.format("%02d", i));
            }

            if (Files.exists(logFile)) {
                String content = Files.readString(logFile);

                // Extract waktu dari setiap block
                String[] blocks = content.split("========================================\n");
                for (String block : blocks) {
                    if (block.contains("🕐 Waktu    :")) {
                        try {
                            int waktuIndex = block.indexOf("🕐 Waktu    :");
                            String timeStr = block.substring(waktuIndex + 13, waktuIndex + 21).trim();
                            int hour = Integer.parseInt(timeStr.substring(0, 2));
                            if (hour >= 0 && hour < 24) {
                                hourlyData[hour]++;
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }

            trends.put("labels", labels);
            trends.put("data", Arrays.stream(hourlyData).boxed().collect(Collectors.toList()));

        } catch (IOException e) {
            trends.put("labels", Arrays.asList("00","01","02","03","04","05","06","07","08","09","10","11",
                    "12","13","14","15","16","17","18","19","20","21","22","23"));
            trends.put("data", Arrays.asList(0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0));
        }

        return trends;
    }

    @GetMapping("/recent")
    public List<Map<String, Object>> getRecentErrors() {
//        List<Map<String, Object>> recent = new ArrayList<>();
//        Map<String, Object> error = new HashMap<>();
//        error.put("time", "14.32");
//        error.put("type", "NullPointerException");
//        error.put("location", "UserService.createUser()");
//        error.put("status", "FIXED");
//        recent.add(error);
//
//

        List<Map<String, Object>> recent = new ArrayList<>();

        try {
            Path logFile = getTodayLogFile();

            if (!Files.exists(logFile)) {
                return recent;
            }

            String content = Files.readString(logFile);
            String[] blocks = content.split("========================================\n");

            // Ambil 10 block terakhir (terbaru)
            for (int i = blocks.length - 1; i >= 0 && recent.size() < 10; i--) {
                String block = blocks[i];

                if (block.contains("❌ ERROR:") || block.contains("Tipe     :")) {
                    Map<String, Object> error = new HashMap<>();

                    // Extract time
                    if (block.contains("🕐 Waktu    :")) {
                        int idx = block.indexOf("🕐 Waktu    :");
                        error.put("time", block.substring(idx + 13, idx + 21).trim());
                    } else {
                        error.put("time", "--:--");
                    }

                    // Extract type
                    if (block.contains("Tipe     :")) {
                        int idx = block.indexOf("Tipe     :");
                        int endIdx = block.indexOf("\n", idx);
                        String type = block.substring(idx + 11, endIdx).trim();
                        type = type.substring(type.lastIndexOf('.') + 1);
                        error.put("type", type);
                    } else {
                        error.put("type", "Unknown");
                    }

                    // Extract location
                    if (block.contains("Lokasi   :")) {
                        int idx = block.indexOf("Lokasi   :");
                        int endIdx = block.indexOf("\n", idx);
                        String loc = block.substring(idx + 11, endIdx).trim();
                        error.put("location", loc);
                    } else {
                        error.put("location", "Unknown");
                    }

                    error.put("status", "PENDING");
                    recent.add(error);
                }
            }

        } catch (IOException e) {
            System.err.println("❌ Failed to read recent: " + e.getMessage());
        }

        return recent;
    }

    @GetMapping("/top-errors")
    public List<Map<String, Object>> getTopErrors() {
//        List<Map<String, Object>> top = new ArrayList<>();
//        Map<String, Object> error = new HashMap<>();
//        error.put("type", "NullPointerException");
//        error.put("count", 23);
//        error.put("percentage", 45);
//        top.add(error);
//
//        return top;

        List<Map<String, Object>> top = new ArrayList<>();
        Map<String, Integer> errorCounts = new HashMap<>();

        try {
            Path logFile = getTodayLogFile();

            if (!Files.exists(logFile)) {
                return top;
            }

            String content = Files.readString(logFile);
            String[] blocks = content.split("========================================\n");

            for (String block : blocks) {
                if (block.contains("Tipe     :")) {
                    int idx = block.indexOf("Tipe     :");
                    int endIdx = block.indexOf("\n", idx);
                    String type = block.substring(idx + 11, endIdx).trim();
                    type = type.substring(type.lastIndexOf('.') + 1);
                    errorCounts.merge(type, 1, Integer::sum);
                }
            }

            int total = errorCounts.values().stream().mapToInt(Integer::intValue).sum();

            errorCounts.entrySet().stream()
                    .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                    .limit(5)
                    .forEach(entry -> {
                        Map<String, Object> error = new HashMap<>();
                        error.put("type", entry.getKey());
                        error.put("count", entry.getValue());
                        error.put("percentage", total > 0 ? (entry.getValue() * 100) / total : 0);
                        top.add(error);
                    });

        } catch (IOException e) {
            System.err.println("❌ Failed to read top errors: " + e.getMessage());
        }

        return top;
    }

    private int countOccurrences(String str, String sub) {
        int count = 0;
        int idx = 0;
        while ((idx = str.indexOf(sub, idx)) != -1) {
            count++;
            idx += sub.length();
        }
        return count;
    }

    private String getSimpleExceptionName(String fullName) {
        if (fullName == null) return "Unknown";
        int lastDot = fullName.lastIndexOf('.');
        return lastDot > 0 ? fullName.substring(lastDot + 1) : fullName;
    }

    private List<String> generateHourLabels() {
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            labels.add(String.format("%02d", i));
        }
        return labels;
    }

    private Path getTodayLogFile() {
        String today = LocalDate.now().format(DATE_FORMAT);
        String normalizedPath = historyService.getLogPath();

        // Coba kedua format
        Path[] possiblePaths = {
                Paths.get(normalizedPath, "error-history-" + today + ".log"),
                Paths.get(normalizedPath, "error-history" + today + ".log")
        };

        for (Path p : possiblePaths) {
            if (Files.exists(p)) {
                System.out.println("📊 Found log file: " + p.getFileName());
                return p;
            }
        }

        // Default
        return possiblePaths[0];
    }
}
