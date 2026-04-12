package com.llm.ai.core.component;

import com.llm.ai.project.debuggingAI.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class SummaryScheduler {

    @Autowired
    private NotificationService notificationService;

    @Value("${notification.summary.enabled:false}")
    private boolean summaryEnabled;

    @Scheduled(cron = "${notification.summary.cron:0 0 * * * *}")
    public void sendSummaryReport() {
        if (summaryEnabled) {
            notificationService.sendSummaryReport();
        }
    }
}