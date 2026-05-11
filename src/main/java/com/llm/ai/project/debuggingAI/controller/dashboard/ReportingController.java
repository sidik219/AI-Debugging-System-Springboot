package com.llm.ai.project.debuggingAI.controller.dashboard;

import com.llm.ai.core.component.DebugSession;
import com.llm.ai.project.debuggingAI.payload.FixReport;
import com.llm.ai.project.debuggingAI.service.NotificationService;
import com.llm.ai.project.debuggingAI.util.SystemInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("api/report")
@CrossOrigin(origins = "*")
public class ReportingController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private DebugSession debugSession;

    @PostMapping("/report-fix")
    public ResponseEntity<?> reportFix(@RequestBody FixReport report) {
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

        notificationService.sendFixReport(report);
        debugSession.recordFixReport(report);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Fix report sent!",
                "deviceName", report.getDeviceName()
        ));
    }

    @GetMapping("/device-info")
    public Map<String, String> getDeviceInfo() {
        return Map.of(
                "deviceName", SystemInfo.getDeviceName(),
                "osName", SystemInfo.getOsName()
        );
    }
}
