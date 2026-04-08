package com.llm.ai.project.debuggingAI.service;

import com.llm.ai.project.debuggingAI.model.AIDebugResponse;
import com.llm.ai.project.debuggingAI.model.ErrorContext;
import com.llm.ai.project.debuggingAI.util.ConsoleColors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AIDebugService {

    @Autowired
    private GroqService groqService;

    @Autowired(required = false)
    private OpenAIService openAIService;

    @Autowired(required = false)
    private GeminiService geminiService;

    @Value("${debug.ai.provider:groq}")
    private String provider;

    @Value("${debug.ai.enabled:true}")
    private boolean aiEnabled;

    public AIDebugResponse analyzeError(ErrorContext errorContext) {
        if (!aiEnabled) {
            System.out.println(ConsoleColors.YELLOW + "⚠️ AI debugging dinonaktifkan" + ConsoleColors.RESET);
            return ruleBasedAnalysis(errorContext);
        }

        System.out.println(ConsoleColors.CYAN + "🤖 Provider AI: " + provider.toUpperCase() + ConsoleColors.RESET);

        try {
            String prompt = buildPrompt(errorContext);
            String aiResponse = callProvider(prompt);

            if (aiResponse != null && !aiResponse.isEmpty()) {
                System.out.println(ConsoleColors.GREEN + "✅ Response dari " + provider + " diterima" + ConsoleColors.RESET);
                return parseAIResponse(aiResponse, errorContext);
            } else {
                System.out.println(ConsoleColors.YELLOW + "⚠️ Tidak ada response dari " + provider + ", fallback ke rule-based" + ConsoleColors.RESET);
                return ruleBasedAnalysis(errorContext);
            }

        } catch (Exception e) {
            System.err.println(ConsoleColors.RED + "❌ Error panggil AI: " + e.getMessage() + ConsoleColors.RESET);
            return ruleBasedAnalysis(errorContext);
        }
    }

    private String callProvider(String prompt) {
        return switch (provider.toLowerCase()) {
            case "groq" -> groqService.analyzeError(prompt).block();
            case "openai" -> openAIService != null ? openAIService.analyzeError(prompt).block() : null;
            case "gemini" -> geminiService != null ? geminiService.analyzeError(prompt) : null;
            default -> {
                System.out.println(ConsoleColors.YELLOW + "⚠️ Provider " + provider + " tidak dikenal, pakai Groq" + ConsoleColors.RESET);
                yield groqService.analyzeError(prompt).block();
            }
        };
    }

    // TODO: Promt B Indo
    private String buildPrompt(ErrorContext errorContext) {
        return String.format("""
        Anda adalah asisten debugging Java Spring Boot. Jawab dalam BAHASA INDONESIA.
        
        ERROR: %s
        PESAN: %s
        LOKASI: %s.%s() baris %d
        
        KODE:
        %s
        
        Berikan:
        ANALISIS: (penyebab error, singkat)
        PERBAIKAN: (langkah-langkah)
        KODE: (contoh perbaikan)
        KEYAKINAN: (HIGH/MEDIUM/LOW)
        """,
                errorContext.getExceptionType(),
                errorContext.getMessage(),
                errorContext.getClassName(),
                errorContext.getMethodName(),
                errorContext.getLineNumber(),
                errorContext.getSourceCode() != null ? errorContext.getSourceCode() : "Tidak tersedia"
        );
    }

    // TODO: Promt B Inggris
//    private String buildPrompt(ErrorContext errorContext) {
//        return String.format("""
//            I encountered this error in my Java Spring Boot application:
//
//            Exception Type: %s
//            Error Message: %s
//            Location: %s.%s() at line %d
//
//            Source code context:
//            %s
//
//            Stack trace (first few lines):
//            %s
//
//            Please provide:
//            1. Root cause analysis (what caused this error)
//            2. Suggested fix (step-by-step solution)
//            3. Code example showing the correct implementation
//            4. Confidence level (HIGH/MEDIUM/LOW)
//
//            Format your response clearly with these sections.
//            """,
//                errorContext.getExceptionType(),
//                errorContext.getMessage(),
//                errorContext.getClassName(),
//                errorContext.getMethodName(),
//                errorContext.getLineNumber(),
//                errorContext.getSourceCode() != null ? errorContext.getSourceCode() : "Not available",
//                errorContext.getStackTrace() != null && !errorContext.getStackTrace().isEmpty() ?
//                        String.join("\n", errorContext.getStackTrace().subList(0,
//                                Math.min(5, errorContext.getStackTrace().size()))) : "Not available"
//        );
//    }

    private AIDebugResponse parseAIResponse(String aiResponse, ErrorContext errorContext) {
        AIDebugResponse response = new AIDebugResponse();

        if (aiResponse == null || aiResponse.isEmpty()) {
            return ruleBasedAnalysis(errorContext);
        }

        aiResponse = aiResponse.replaceAll("\033\\[[0-9;]*m", "");

        try {
            String[] sections = aiResponse.split("(?i)(?=ANALISIS:|ANALYSIS:|PERBAIKAN:|FIX:|KODE:|CODE:|KEYAKINAN:|CONFIDENCE:)");

            for (String section : sections) {
                String upperSection = section.toUpperCase();

                if (upperSection.contains("ANALISIS:") || upperSection.contains("ANALYSIS:")) {
                    response.setAnalysis(section.replaceFirst("(?i)ANALISIS:|ANALYSIS:", "").trim());
                } else if (upperSection.contains("PERBAIKAN:") || upperSection.contains("FIX:")) {
                    response.setSuggestedFix(section.replaceFirst("(?i)PERBAIKAN:|FIX:", "").trim());
                } else if (upperSection.contains("KODE:") || upperSection.contains("CODE:")) {
                    response.setCodeExample(section.replaceFirst("(?i)KODE:|CODE:", "").trim());
                } else if (upperSection.contains("KEYAKINAN:") || upperSection.contains("CONFIDENCE:")) {
                    String conf = section.replaceFirst("(?i)KEYAKINAN:|CONFIDENCE:", "").trim().toUpperCase();
                    if (conf.contains("HIGH") || conf.contains("TINGGI")) {
                        response.setConfidence("HIGH");
                    } else if (conf.contains("MEDIUM") || conf.contains("SEDANG")) {
                        response.setConfidence("MEDIUM");
                    } else {
                        response.setConfidence("LOW");
                    }
                }
            }

            if (response.getAnalysis() == null) response.setAnalysis(aiResponse);
            if (response.getSuggestedFix() == null) response.setSuggestedFix("Lihat respons AI di atas");
            if (response.getConfidence() == null) response.setConfidence("MEDIUM");

        } catch (Exception e) {
            response.setAnalysis(aiResponse);
            response.setSuggestedFix("Parse respons AI secara manual");
            response.setConfidence("LOW");
        }

        return response;
    }

    private AIDebugResponse ruleBasedAnalysis(ErrorContext errorContext) {
        AIDebugResponse response = new AIDebugResponse();
        String exceptionType = errorContext.getExceptionType();

        if (exceptionType.contains("NullPointerException")) {
            response.setAnalysis("Anda mencoba menggunakan objek yang bernilai null.");
            response.setSuggestedFix("Tambahkan pengecekan null atau gunakan Optional.");
            response.setCodeExample("""
                if (obj != null) {
                    return obj.method();
                }
                return "";
                """);
            response.setConfidence("HIGH");
        } else {
            response.setAnalysis("Exception: " + exceptionType);
            response.setSuggestedFix("Periksa stack trace dan debug secara manual.");
            response.setConfidence("LOW");
        }

        return response;
    }
}