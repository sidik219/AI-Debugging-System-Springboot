package com.llm.ai.project.debuggingAI.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {
    private String customerName;
    private String productCode;
    private Integer quantity;
    private String paymentMethod;
}
