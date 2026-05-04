package com.llm.ai.project.debuggingAI.controller;

import com.llm.ai.project.debuggingAI.model.TestEntity;
import com.llm.ai.project.debuggingAI.payload.OrderRequest;
import com.llm.ai.project.debuggingAI.response.OrderResponse;
import com.llm.ai.project.debuggingAI.service.OrderService;
import com.llm.ai.project.debuggingAI.service.TestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ekosistem")
public class EkosistemTestController {

    @Autowired
    private TestService testService;

    @Autowired
    private OrderService orderService;

    // TODO: ==================== Semua Ekosistem Umum Test ====================

    @GetMapping("/default/service-error")
    public TestEntity testServiceError(@RequestParam String name, @RequestParam(defaultValue = "10") Integer value) {
        return testService.createEntity(name, value);
    }

    @GetMapping("/default/repo-error")
    public TestEntity testRepoError(@RequestParam(defaultValue = "1") Long id) {
        return testService.getEntityById(id);
    }

    @GetMapping("/default/delete-error")
    public String testDeleteError(@RequestParam Long id) {
        testService.deleteEntity(id);
        return "Deleted";
    }

    @GetMapping("/default/calc-error")
    public Integer testCalcError(@RequestParam Long id, @RequestParam(defaultValue = "0") Integer multiplier) {
        return testService.calculateValue(id, multiplier);
    }

    // TODO: ==================== Semua Ekosistem Spesifik Test ====================

    @PostMapping("/spesifik/create-orders")
    public ResponseEntity<OrderResponse> createOrder(@RequestBody OrderRequest request) {
        OrderResponse response = orderService.processOrder(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/spesifik/order")
    public ResponseEntity<OrderResponse> testOrder(
            @RequestParam String customer,
            @RequestParam String product,
            @RequestParam Integer qty,
            @RequestParam String payment) {

        OrderRequest request = new OrderRequest();
        request.setCustomerName(customer);
        request.setProductCode(product);
        request.setQuantity(qty);
        request.setPaymentMethod(payment);

        OrderResponse response = orderService.processOrder(request);
        return ResponseEntity.ok(response);
    }
}
