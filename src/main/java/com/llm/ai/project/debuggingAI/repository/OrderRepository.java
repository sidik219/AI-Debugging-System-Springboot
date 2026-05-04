package com.llm.ai.project.debuggingAI.repository;

import com.llm.ai.project.debuggingAI.model.OrderEntity;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class OrderRepository {

    private final Map<Long, OrderEntity> db = new ConcurrentHashMap<>();
    private long idCounter = 1000;

    private static final Map<String, ProductInfo> productCatalog = new HashMap<>();
    static {
        productCatalog.put("PRD-001", new ProductInfo("Laptop", 15000000.0, 10));
        productCatalog.put("PRD-002", new ProductInfo("Mouse", 250000.0, 50));
        productCatalog.put("PRD-003", new ProductInfo("Keyboard", 750000.0, 0));
        productCatalog.put("PRD-004", new ProductInfo("Monitor", 3500000.0, 5));
        productCatalog.put("PRD-999", new ProductInfo("Special Item", 9999999.0, 1));
    }

    public OrderEntity save(OrderEntity order) {
        if (order.getQuantity() > 100) {
            throw new RuntimeException("Database error: Disk full - cannot save order with quantity " + order.getQuantity());
        }

        order.setOrderId((++idCounter));
        order.setCreatedAt(LocalDateTime.now());
        return order;
    }

    public Optional<ProductInfo> findProductByCode(String productCode) {
        if ("PRD-999".equals(productCode)) {
            throw new RuntimeException("Database connection timeout while querying product: " + productCode);
        }
        return Optional.ofNullable(productCatalog.get(productCode));
    }

    public void updateStock(String productCode, Integer quantity) {
        ProductInfo product = productCatalog.get(productCode);
        if (product != null) {
            if (quantity > product.getStock()) {
                throw new IllegalStateException("Stock insufficient! Requested: " + quantity + ", Available: " + product.getStock());
            }
            product.setStock(product.getStock() - quantity);
        }
    }

    public static class ProductInfo {
        private String name;
        private Double price;
        private Integer stock;

        public ProductInfo(String name, Double price, Integer stock) {
            this.name = name;
            this.price = price;
            this.stock = stock;
        }

        public String getName() { return name; }
        public Double getPrice() { return price; }
        public Integer getStock() { return stock; }
        public void setStock(Integer stock) { this.stock = stock; }
    }
}
