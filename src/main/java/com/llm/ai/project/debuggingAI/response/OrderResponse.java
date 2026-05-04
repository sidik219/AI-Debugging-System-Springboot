package com.llm.ai.project.debuggingAI.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderResponse {
    private boolean success;
    private String message;
    private Long orderId;
    private String productName;
    private Double totalPrice;
    private String paymentMethod;
    private String status;
    private LocalDateTime processedAt;
}
