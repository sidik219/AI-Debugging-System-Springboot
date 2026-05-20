package com.llm.ai.project.debuggingAI.common.model;

import lombok.Data;

@Data
public class AIDebugResponse {
    private String analysis;
    private String suggestedFix;
    private String codeExample;
    private String confidence;
}
