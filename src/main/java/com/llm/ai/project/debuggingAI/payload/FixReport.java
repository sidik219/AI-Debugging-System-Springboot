package com.llm.ai.project.debuggingAI.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FixReport {
    private String developerName;
    private String deviceName;
    private String methodName;
    private String description;
    private String status;
    private String level;
}
