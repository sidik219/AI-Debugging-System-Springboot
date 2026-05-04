package com.llm.ai.project.debuggingAI.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OrderEntity {
    private Long orderId;
    private String customerName;
    private String productCode;
    private Integer quantity;
    private Double totalPrice;
    private String paymentMethod;
    private String status;
    private LocalDateTime createdAt;
}
