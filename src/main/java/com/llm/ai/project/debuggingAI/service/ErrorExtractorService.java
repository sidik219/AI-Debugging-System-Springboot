package com.llm.ai.project.debuggingAI.service;

import com.llm.ai.project.debuggingAI.model.ErrorContext;
import com.llm.ai.project.debuggingAI.util.ConsoleColors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ErrorExtractorService {

    @Value("${debug.base-package:}")
    private String configuredBasePackage;

    @Autowired
    private ApplicationContext applicationContext;

    private String cachedBasePackage = null;

    private String detectBasePackage(StackTraceElement[] stackTrace) {
        // 1. Return cached jika sudah ada
        if (cachedBasePackage != null) {
            return cachedBasePackage;
        }

        // 2. Pakai konfigurasi user jika ada
        if (configuredBasePackage != null && !configuredBasePackage.isEmpty()) {
            System.out.println(ConsoleColors.CYAN + "📦 Base package (configured): " + configuredBasePackage + ConsoleColors.RESET);
            cachedBasePackage = configuredBasePackage;
            return cachedBasePackage;
        }

        // 3. Deteksi dari main class (@SpringBootApplication)
        String mainPackage = detectFromMainClass();
        if (mainPackage != null) {
            System.out.println(ConsoleColors.CYAN + "📦 Base package (detected from main): " + mainPackage + ConsoleColors.RESET);
            cachedBasePackage = mainPackage;
            return cachedBasePackage;
        }

        // 4. Deteksi dari stack trace
        String stackPackage = detectFromStackTrace(stackTrace);
        if (stackPackage != null) {
            System.out.println(ConsoleColors.CYAN + "📦 Base package (detected from stack): " + stackPackage + ConsoleColors.RESET);
            cachedBasePackage = stackPackage;
            return cachedBasePackage;
        }

        // 5. Fallback
        cachedBasePackage = "com.example";
        System.out.println(ConsoleColors.YELLOW + "⚠️ Base package not detected, using fallback: " + cachedBasePackage + ConsoleColors.RESET);
        return cachedBasePackage;
    }

    private String detectFromMainClass() {
        try {
            // Coba cari @SpringBootApplication
            Map<String, Object> beans = applicationContext.getBeansWithAnnotation(SpringBootApplication.class);

            // Fallback ke @SpringBootConfiguration
            if (beans.isEmpty()) {
                beans = applicationContext.getBeansWithAnnotation(SpringBootConfiguration.class);
            }

            if (!beans.isEmpty()) {
                Object mainApp = beans.values().iterator().next();
                Class<?> mainClass = mainApp.getClass();

                // Handle CGLIB proxy
                if (mainClass.getName().contains("$$")) {
                    mainClass = mainClass.getSuperclass();
                }

                String packageName = mainClass.getPackage().getName();
                return extractBasePackage(packageName);
            }
        } catch (Exception e) {
            // ApplicationContext mungkin belum siap saat startup awal
            // Akan fallback ke method berikutnya
        }
        return null;
    }

    private String detectFromStackTrace(StackTraceElement[] stackTrace) {
        if (stackTrace == null || stackTrace.length == 0) return null;

        Map<String, Integer> packageCount = new HashMap<>();

        // Package yang di-exclude (library/framework)
        Set<String> excludedPrefixes = Set.of(
                "java.", "javax.", "jakarta.",
                "org.springframework.", "org.apache.", "org.hibernate.",
                "com.sun.", "sun.", "jdk.",
                "com.fasterxml.", "org.aspectj.", "org.mockito.",
                "net.bytebuddy.", "io.micrometer.", "reactor.",
                "com.llm.ai"
        );

        for (StackTraceElement element : stackTrace) {
            String className = element.getClassName();
            int lastDot = className.lastIndexOf('.');
            if (lastDot <= 0) continue;

            String pkg = className.substring(0, lastDot);

            // Skip excluded packages
            boolean excluded = false;
            for (String prefix : excludedPrefixes) {
                if (pkg.startsWith(prefix)) {
                    excluded = true;
                    break;
                }
            }
            if (excluded) continue;

            // Extract base package (2-3 levels)
            String basePkg = extractBasePackage(pkg);
            packageCount.merge(basePkg, 1, Integer::sum);
        }

        // Ambil package dengan count terbanyak
        return packageCount.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private String extractBasePackage(String fullPackage) {
        String[] parts = fullPackage.split("\\.");
        if (parts.length <= 2) return fullPackage;
        if (parts.length == 3) return fullPackage;
        // Ambil 3 level pertama
        return parts[0] + "." + parts[1] + "." + parts[2];
    }

    public void resetBasePackageCache() {
        cachedBasePackage = null;
    }

    public String getCurrentBasePackage() {
        return cachedBasePackage != null ? cachedBasePackage : "not detected yet";
    }

    public ErrorContext extractErrorContext(Throwable exception) {
        ErrorContext context = new ErrorContext();

        // Basic error info
        context.setExceptionType(exception.getClass().getName());
        context.setMessage(exception.getMessage() != null ?
                exception.getMessage() : "No message available");
        context.setTimestamp(LocalDateTime.now());

        // Extract stack trace
        StackTraceElement[] stackTrace = exception.getStackTrace();
        if (stackTrace != null && stackTrace.length > 0) {
            List<String> stackTraceList = Arrays.stream(stackTrace)
                    .map(element -> String.format("at %s.%s(%s:%d)",
                            element.getClassName(),
                            element.getMethodName(),
                            element.getFileName() != null ? element.getFileName() : "Unknown",
                            element.getLineNumber()))
                    .collect(Collectors.toList());
            context.setStackTrace(stackTraceList);

            // Deteksi base package
            String basePackage = detectBasePackage(stackTrace);

            System.out.println("\n🔍 DEBUG: Base package = " + basePackage);
            System.out.println("   Searching stack trace...");

            // Print 5 frame pertama untuk debugging
            for (int i = 0; i < Math.min(stackTrace.length, 5); i++) {
                StackTraceElement element = stackTrace[i];
                System.out.println("   [" + i + "] " + element.getClassName() + "." + element.getMethodName() + "()");
            }

            // Cari stack trace dari base package
            for (StackTraceElement element : stackTrace) {
                String className = element.getClassName();

                if (className != null && className.startsWith(basePackage)) {
                    context.setClassName(className);
                    context.setMethodName(element.getMethodName());
                    context.setLineNumber(element.getLineNumber());
                    context.setFileName(element.getFileName());

                    System.out.println("   📂 Found: " + className + "." + element.getMethodName() + "()");
                    context.setSourceCode(readSourceCode(element));
                    return context;
                }
            }

            // Fallback: ambil stack trace pertama yang bukan dari library/framework
            System.out.println("   ⚠️ No stack trace in base package, using first non-library frame");

            Set<String> excludedPrefixes = Set.of(
                    "java.", "javax.", "jakarta.", "org.springframework.",
                    "org.apache.", "org.hibernate.", "com.sun.", "sun.", "jdk.",
                    "com.llm.ai"
            );

            for (StackTraceElement element : stackTrace) {
                String className = element.getClassName();
                boolean excluded = false;
                for (String prefix : excludedPrefixes) {
                    if (className.startsWith(prefix)) {
                        excluded = true;
                        break;
                    }
                }
                if (!excluded) {
                    context.setClassName(className);
                    context.setMethodName(element.getMethodName());
                    context.setLineNumber(element.getLineNumber());
                    context.setFileName(element.getFileName());
                    context.setSourceCode(readSourceCode(element));
                    return context;
                }
            }

            // Last resort: first element
            StackTraceElement firstElement = stackTrace[0];
            context.setClassName(firstElement.getClassName());
            context.setMethodName(firstElement.getMethodName());
            context.setLineNumber(firstElement.getLineNumber());
            context.setFileName(firstElement.getFileName());
            context.setSourceCode(readSourceCode(firstElement));
            return context;
        }

        // Default values jika tidak ada stack trace
        context.setClassName("Unknown");
        context.setMethodName("Unknown");
        context.setLineNumber(0);
        context.setSourceCode("No stack trace available");
        context.setStackTrace(new ArrayList<>());

        return context;
    }

    private String readSourceCode(StackTraceElement element) {
        if (element == null || element.getClassName() == null) {
            return "Source code not available";
        }

        String className = element.getClassName();

        try {
            String[] possiblePaths = {
                    "src/main/java/" + className.replace('.', '/') + ".java",
                    "./src/main/java/" + className.replace('.', '/') + ".java",
                    "../src/main/java/" + className.replace('.', '/') + ".java"
            };

            for (String filePath : possiblePaths) {
                Path path = Paths.get(filePath);
                if (Files.exists(path)) {
                    List<String> lines = Files.readAllLines(path);
                    int errorLine = element.getLineNumber();

                    int startLine = Math.max(0, errorLine - 8);
                    int endLine = Math.min(lines.size(), errorLine + 8);

                    StringBuilder sourceCode = new StringBuilder();
                    sourceCode.append(String.format("📍 File: %s%n",
                            element.getFileName() != null ? element.getFileName() : className));
                    sourceCode.append(String.format("📍 Error at line %d:%n%n", errorLine));

                    for (int i = startLine; i < endLine; i++) {
                        String marker = (i + 1 == errorLine) ? "👉" : "  ";
                        sourceCode.append(String.format("%s %4d: %s%n",
                                marker, i + 1, lines.get(i)));
                    }
                    return sourceCode.toString();
                }
            }

            System.out.println("   ⚠️ Source file not found for: " + className);
        } catch (IOException e) {
            System.out.println("   ❌ Error reading source: " + e.getMessage());
        }

        return String.format("""
            📄 Class: %s
            🔧 Method: %s()
            📍 Line: %d
            
            💡 Source file not found. Check the code in your IDE.
            """,
                className,
                element.getMethodName(),
                element.getLineNumber()
        );
    }
}
