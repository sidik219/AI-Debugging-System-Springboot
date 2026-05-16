package com.llm.ai.project.debuggingAI.controller.dashboard;

import com.llm.ai.core.component.DebugSession;
import com.llm.ai.project.debuggingAI.payload.FixReport;
import com.llm.ai.project.debuggingAI.service.NotificationService;
import com.llm.ai.project.debuggingAI.service.ReportingService;
import com.llm.ai.project.debuggingAI.util.SystemInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/report")
public class ReportingController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private DebugSession debugSession;

    @Autowired
    private ReportingService reportingService;

    @PostMapping("/send-report")
    public ResponseEntity<?> sendReport(@RequestBody FixReport report) {
        report.setDeviceName(SystemInfo.getDeviceName());

        if (report.getMethodName() == null || report.getMethodName().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Method name is required!"
            ));
        }
        if (report.getDeveloperName() == null || report.getDeveloperName().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Developer name is required!"
            ));
        }
        if (report.getStatus() == null || report.getStatus().trim().isEmpty()) {
            report.setStatus("SELESAI");
        }
        if (report.getLevel() == null || report.getLevel().trim().isEmpty()) {
            report.setLevel("NORMAL");
        }

        Map<String, Object> saved = reportingService.saveReport(report);

        notificationService.sendFixReport(report);
        debugSession.recordFixReport(report);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Fix report sent!",
                "data", saved
        ));
    }

    @GetMapping("/filter-reports")
    public ResponseEntity<?> getFilterReports(
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String status) {
        List<Map<String, Object>> reports;

        if ((level != null && !level.equals("ALL")) || (status != null && !status.equals("ALL"))) {
            reports = reportingService.findByFilter(level, status);
        } else {
            reports = reportingService.findAll();
        }

        return ResponseEntity.ok(Map.of(
                "total", reports.size(),
                "reports", reports
        ));
    }

    @DeleteMapping("/delete-reports")
    public ResponseEntity<?> deleteAllReports() {
        reportingService.clearAll();
        return ResponseEntity.ok(Map.of("success", true, "message", "All reports cleared"));
    }

    @DeleteMapping("/delete-report/{id}")
    public ResponseEntity<?> deleteByIdReport(@PathVariable long id) {
        boolean deleted = reportingService.deleteById(id);
        if (deleted) {
            return ResponseEntity.ok(Map.of("success", true, "message", "Report deleted"));
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/device-info")
    public Map<String, String> getDeviceInfo() {
        return Map.of(
                "deviceName", SystemInfo.getDeviceName(),
                "osName", SystemInfo.getOsName()
        );
    }
}
