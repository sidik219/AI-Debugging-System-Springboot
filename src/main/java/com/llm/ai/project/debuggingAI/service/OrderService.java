package com.llm.ai.project.debuggingAI.service;

import com.llm.ai.project.debuggingAI.model.OrderEntity;
import com.llm.ai.project.debuggingAI.payload.OrderRequest;
import com.llm.ai.project.debuggingAI.repository.OrderRepository;
import com.llm.ai.project.debuggingAI.response.OrderResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    public OrderResponse processOrder(OrderRequest request) {
        validateRequest(request);
        validatePaymentMethod(request.getPaymentMethod());

        OrderRepository.ProductInfo product = orderRepository.findProductByCode(request.getProductCode())
                .orElseThrow(() -> new NullPointerException("Product not found: " + request.getProductCode()));

        validateStock(product, request.getQuantity());

        Double totalPrice = product.getPrice() * request.getQuantity();

        validatePaymentLimit(totalPrice);

        OrderEntity order = buildOrderEntity(request, totalPrice);
        OrderEntity saveOrder = orderRepository.save(order);
        orderRepository.updateStock(request.getProductCode(), request.getQuantity());

        return buildSuccessResponse(saveOrder, product, totalPrice);
    }

    private void validateRequest(OrderRequest request) {
        if (request.getCustomerName() == null || request.getCustomerName().trim().isEmpty()) {
            throw new IllegalArgumentException("Customer name is required!");
        }
        if (request.getProductCode() == null || request.getProductCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Product code is required!");
        }
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0!");
        }
        if (request.getPaymentMethod() == null || request.getPaymentMethod().trim().isEmpty()) {
            throw new IllegalArgumentException("Payment method is required!");
        }
    }

    private void validatePaymentMethod(String paymentMethod) {
        boolean isValid = paymentMethod.equalsIgnoreCase("TRANSFER") ||
                          paymentMethod.equalsIgnoreCase("EWALLET") ||
                          paymentMethod.equalsIgnoreCase("COD");

        if (!isValid) {
            throw new IllegalArgumentException(
                    "Invalid payment method: " +
                            paymentMethod +
                    ". Accepted: TRANSFER, EWALLET, COD"
            );
        }
    }

    private void validateStock(OrderRepository.ProductInfo product, Integer quantity) {
        if (product.getStock() <= 0) {
            throw new IllegalStateException("Product out of stock: " + product.getName());
        }
        if (quantity > product.getStock()) {
            throw new IllegalStateException(
                    "Insufficient stock! Available: "
                            + product.getStock()
                            + ", Requested: "
                            + quantity);
        }
    }

    private void validatePaymentLimit(Double totalPrice) {
        if (totalPrice > 10000000) {
            throw new RuntimeException(
                    "Payment gateway timeout! Total exceeds limit: Rp "
                    + String.format("%,.0f", totalPrice)
            );
        }
    }

    private OrderEntity buildOrderEntity(OrderRequest request, Double totalPrice) {
        OrderEntity order = new OrderEntity();
        order.setCustomerName(request.getCustomerName());
        order.setProductCode(request.getProductCode());
        order.setQuantity(request.getQuantity());
        order.setTotalPrice(totalPrice);
        order.setPaymentMethod(request.getPaymentMethod());
        order.setStatus("PROCESSING");

        return order;
    }

    private OrderResponse buildSuccessResponse(OrderEntity saveOrder, OrderRepository.ProductInfo product, Double totalPrice) {
        return OrderResponse.builder()
                .success(true)
                .message("Order created successfully!")
                .orderId(saveOrder.getOrderId())
                .productName(product.getName())
                .totalPrice(totalPrice)
                .paymentMethod(saveOrder.getPaymentMethod())
                .status(saveOrder.getStatus())
                .processedAt(LocalDateTime.now())
                .build();
    }
}
