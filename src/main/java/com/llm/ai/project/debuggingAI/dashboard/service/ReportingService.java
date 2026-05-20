package com.llm.ai.project.debuggingAI.dashboard.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.llm.ai.project.debuggingAI.dashboard.dto.payload.FixReport;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class ReportingService {

    @Value("${debug.storage.path:./error-logs}")
    private String storagePath;

    private final ObjectMapper objectMapper;
    private final List<Map<String, Object>> reports = new CopyOnWriteArrayList<>();
    private long idCounter = 1;

    public ReportingService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @PostConstruct
    public void loadFromFile() {
        try {
            File file = new File(storagePath + "/fix-reports.json");
            if (file.exists()) {
                List<Map<String, Object>> loaded = objectMapper.readValue(file, new TypeReference<List<Map<String, Object>>>() {});
                reports.addAll(loaded);

                if (!reports.isEmpty()) {
                    idCounter = reports.stream()
                            .mapToLong(r -> ((Number) r.getOrDefault("id", 0L)).longValue())
                            .max()
                            .orElse(0) + 1;
                }
                System.out.println("📂 Fix reports loaded: " + reports.size());
            }
        } catch (IOException e) {
            System.err.println("⚠️ Failed to load fix reports: " + e.getMessage());
        }
    }

    public Map<String, Object> saveReport(FixReport report) {
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("id", idCounter++);
        record.put("developerName", report.getDeveloperName());
        record.put("deviceName", report.getDeviceName());
        record.put("methodName", report.getMethodName());
        record.put("description", report.getDescription());
        record.put("level", report.getLevel() != null ? report.getLevel() : "NORMAL");
        record.put("status", report.getStatus() != null ? report.getStatus() : "SELESAI");
        record.put("timestamp", LocalDateTime.now().toString());
        reports.add(record);

        saveToFile();

        return record;
    }

    public List<Map<String, Object>> findAll() {
        return new ArrayList<>(reports);
    }

    public List<Map<String, Object>> findByFilter(String level, String status) {
        return reports.stream()
                .filter(r -> level == null || level.equals("ALL") || level.equals(r.get("level")))
                .filter(r -> status == null || status.equals("ALL") || status.equals(r.get("status")))
                .collect(java.util.stream.Collectors.toList());
    }

    public long count() {
        return reports.size();
    }

    public void clearAll() {
        reports.clear();
        saveToFile();
    }

    public boolean deleteById(long id) {
        boolean removed = reports.removeIf(r -> {
            Object reportId = r.get("id");
            return reportId != null && ((Number) reportId).longValue() == id;
        });

        if (removed) {
            saveToFile();
            System.out.println("🗑️ Report deleted: ID " + id);
        }

        return removed;
    }

    private void saveToFile() {
        try {
            File dir = new File(storagePath);
            if (!dir.exists()) dir.mkdirs();

            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(new File(storagePath + "/fix-reports.json"), reports);
        } catch (IOException e) {
            System.err.println("❌ Failed to save fix reports: " + e.getMessage());
        }
    }
}
