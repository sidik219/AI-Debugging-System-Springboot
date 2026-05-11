package com.llm.ai.project.debuggingAI.controller.admin;

import com.llm.ai.project.debuggingAI.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/notif")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @GetMapping("/notification-status")
    public Map<String, Object> notificationStatus() {
        return notificationService.getRateLimitStatus();
    }

    @GetMapping("/send-summary")
    public String sendSummary() {
        notificationService.sendSummaryReport();
        return "✅ Summary sent";
    }

    @GetMapping("/summary-stats")
    public Map<String, Object> summaryStats() {
        return notificationService.getSummaryStats();
    }

    @GetMapping("/clear-summaries")
    public String clearSummaries() {
        notificationService.clearSummaries();
        return "✅ Summaries cleared";
    }
}
