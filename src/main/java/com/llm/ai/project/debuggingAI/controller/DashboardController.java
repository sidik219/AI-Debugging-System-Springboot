package com.llm.ai.project.debuggingAI.controller;

import com.llm.ai.project.debuggingAI.model.ErrorContext;
import com.llm.ai.project.debuggingAI.service.ErrorHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Array;
import java.util.*;

@RestController
@RequestMapping("api/dashboard")
@CrossOrigin(origins = "*")
public class DashboardController {

    @Autowired
    private ErrorHistoryService historyService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public void broadcastNewError(ErrorContext context, String analysis, String fix) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("timestamp", context.getTimestamp().toString());
        payload.put("type", context.getExceptionType());
        payload.put("message", context.getMessage());
        payload.put("location", context.getClassName() + "." + context.getMethodName() + "()");
        payload.put("analysis", analysis);
        payload.put("fix", fix);

        messagingTemplate.convertAndSend("/topic/errors", payload);
    }

    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalToday", 47);
        stats.put("fixedToday", 38);
        stats.put("successRate", 81);
        stats.put("avgResponseTime", "2.3s");

        return stats;
    }

    @GetMapping("/trends")
    public Map<String, Object> getTrends() {
        Map<String, Object> trends = new HashMap<>();

        List<String> labels = Arrays.asList(
                "00", "02", "04", "06", "08",
                "10", "12", "14", "16", "18",
                "20", "22"
        );
        List<Integer> data = Arrays.asList(
                2, 1, 0, 3, 5,
                8, 12, 15, 10,
                7, 4, 2
        );

        trends.put("labels", labels);
        trends.put("data", data);

        return trends;
    }

    @GetMapping("/recent")
    public List<Map<String, Object>> getRecentErrors() {
        List<Map<String, Object>> recent = new ArrayList<>();
        Map<String, Object> error = new HashMap<>();
        error.put("time", "14.32");
        error.put("type", "NullPointerException");
        error.put("location", "UserService.createUser()");
        error.put("status", "FIXED");
        recent.add(error);

        return recent;
    }

    @GetMapping("/top-errors")
    public List<Map<String, Object>> getTopErrors() {
        List<Map<String, Object>> top = new ArrayList<>();
        Map<String, Object> error = new HashMap<>();
        error.put("type", "NullPointerException");
        error.put("count", 23);
        error.put("percentage", 45);
        top.add(error);

        return top;
    }
}
