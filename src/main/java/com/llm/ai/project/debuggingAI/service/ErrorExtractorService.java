package com.llm.ai.project.debuggingAI.service;

import com.llm.ai.project.debuggingAI.model.ErrorContext;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ErrorExtractorService {

    public ErrorContext extractErrorContext(Throwable exception) {
        ErrorContext context = new ErrorContext();

        context.setExceptionType(exception.getClass().getName());
        context.setMessage(exception.getMessage() != null ?
                exception.getMessage() : "No message available");
        context.setTimestamp(LocalDateTime.now());

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

            System.out.println("\n🔍 DEBUG: Searching for stack trace with package 'com.llm.ai'");

            for (int i = 0; i < stackTrace.length; i++) {
                StackTraceElement element = stackTrace[i];
                String className = element.getClassName();

                if (i < 5) {
                    System.out.println("   [" + i + "] " + className + "." + element.getMethodName() + "()");
                }

                if (className != null && className.startsWith("com.llm.ai")) {
                    context.setClassName(className);
                    context.setMethodName(element.getMethodName());
                    context.setLineNumber(element.getLineNumber());
                    context.setFileName(element.getFileName());
                    context.setSourceCode(readSourceCode(element));
                    return context;
                }
            }

            StackTraceElement firstElement = stackTrace[0];
            context.setClassName(firstElement.getClassName());
            context.setMethodName(firstElement.getMethodName());
            context.setLineNumber(firstElement.getLineNumber());
            context.setFileName(firstElement.getFileName());
            context.setSourceCode(readSourceCode(firstElement));
            return context;
        }

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
            String filePath = "src/main/java/" + className.replace('.', '/') + ".java";
            Path path = Paths.get(filePath);

            if (Files.exists(path)) {
                System.out.println("   📂 Found source file: " + filePath);

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
