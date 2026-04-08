package com.llm.ai.project.debuggingAI.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ErrorContext {
    private String exceptionType;
    private String message;
    private List<String> stackTrace;
    private String className;
    private String methodName;
    private String fileName;
    private int lineNumber;
    private LocalDateTime timestamp;
    private String sourceCode;
}
