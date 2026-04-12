package com.llm.ai.project.debuggingAI.service;

import com.llm.ai.core.common.UnitTestGenerator;
import com.llm.ai.core.component.DebugSession;
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

    @Autowired
    private DebugSession debugSession;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UnitTestGenerator unitTestGenerator;

    @Value("${debug.ai.providers:groq,openai,gemini}")
    private String provider;

    @Value("${debug.ai.enabled:true}")
    private boolean aiEnabled;

    @Value("${debug.session.enabled:true}")
    private boolean sessionEnabled;

    @Value("${debug.autofix.enabled:false}")
    private boolean autoFixEnabled;

    private String lastSuccessfulProvider = null;

    public AIDebugResponse analyzeError(ErrorContext errorContext) {
        if (!aiEnabled) {
            System.out.println(ConsoleColors.YELLOW + "⚠️ AI debugging dinonaktifkan" + ConsoleColors.RESET);
            return ruleBasedAnalysis(errorContext);
        }

        // Cek cached solution
        if (sessionEnabled) {
            String cached = debugSession.getCachedSolution(errorContext);
            if (cached != null) {
                System.out.println(ConsoleColors.GREEN + "✅ Menggunakan solusi tersimpan yang sudah terbukti berhasil!" + ConsoleColors.RESET);
                AIDebugResponse cachedResponse = new AIDebugResponse();
                cachedResponse.setAnalysis("(Solusi tersimpan)");
                cachedResponse.setSuggestedFix(cached);
                cachedResponse.setConfidence("HIGH");
                return cachedResponse;
            }
        }

        System.out.println(ConsoleColors.CYAN + "🤖 Provider AI: " + provider.toUpperCase() + ConsoleColors.RESET);

        int attemptCount = sessionEnabled ? debugSession.getAttemptCount(errorContext) + 1 : 1;
        System.out.println(ConsoleColors.CYAN + "🔄 Attempt #" + attemptCount + ConsoleColors.RESET);

        try {
            String prompt = buildPrompt(errorContext);
            String aiResponse = callProviderWithFallback(prompt);
//            String aiResponse = callProvider(prompt);

            if (aiResponse != null && !aiResponse.isEmpty()) {
                String successProvider = getLastSuccessfulProvider();
                System.out.println(ConsoleColors.GREEN + "✅ Response dari " + successProvider + " diterima" + ConsoleColors.RESET);
                AIDebugResponse response = parseAIResponse(aiResponse, errorContext);

                if (sessionEnabled) {
                    debugSession.recordAttempt(errorContext, response);
                }

                return response;
            } else {
                System.out.println(ConsoleColors.YELLOW + "⚠️ Tidak ada response, fallback ke rule-based" + ConsoleColors.RESET);
                return ruleBasedAnalysis(errorContext);
            }

        } catch (Exception e) {
            System.err.println(ConsoleColors.RED + "❌ Error panggil AI: " + e.getMessage() + ConsoleColors.RESET);
            return ruleBasedAnalysis(errorContext);
        }
    }

    public void markAsFixed(ErrorContext context, AIDebugResponse response) {
        if (sessionEnabled) {
            debugSession.recordSuccess(context, response);
            notificationService.sendSuccessNotification(context, debugSession.getAttemptCount(context));
        }
    }

    public String generateTest(ErrorContext context) {
        return unitTestGenerator.generateUnitTest(context);
    }

    public String generateAllTests(String className) {
        return unitTestGenerator.generateAllUnitTests(className);
    }

    public void sendNotification(ErrorContext context, AIDebugResponse response) {
        notificationService.sendErrorNotification(context, response, provider);
    }

    public void clearSession(ErrorContext context) {
        if (sessionEnabled) {
            debugSession.clearSession(context);
        }
    }

    private String callProviderWithFallback(String prompt) {
        String[] providers = provider.split(",");

        for (String p : providers) {
            String providerName = p.trim().toLowerCase();
            System.out.println(ConsoleColors.CYAN + "   🚀 Trying provider: " + providerName + ConsoleColors.RESET);

            try {
                String response = callSpesificProvider(providerName, prompt);
                if (response != null && !response.isEmpty()) {
                    System.out.println(ConsoleColors.GREEN + "   ✅ " + providerName + " SUCCESS" + ConsoleColors.RESET);
                    lastSuccessfulProvider = providerName;

                    return response;
                }
            } catch (Exception e) {
                System.err.println(ConsoleColors.YELLOW + "   ⚠️ " + providerName + " failed: " + e.getMessage() + ConsoleColors.RESET);
            }
        }

        System.out.println(ConsoleColors.RED + "   ❌ All providers failed!" + ConsoleColors.RESET);
        return null;
    }

    private String callSpesificProvider(String providerName, String prompt) {
        return switch (providerName) {
            case "groq" -> groqService.analyzeError(prompt).block();
            case "openai" -> openAIService != null ? openAIService.analyzeError(prompt).block() : null;
            case "gemini" -> geminiService != null ? geminiService.analyzeError(prompt) : null;
            default -> null;
        };
    }

    private String getLastSuccessfulProvider() {
        return lastSuccessfulProvider != null ? lastSuccessfulProvider : "unknown";
    }

    // TODO: Old Code
//    private String callProvider(String prompt) {
//        try {
//            return switch (provider.toLowerCase()) {
//                case "groq" -> groqService.analyzeError(prompt).block();
//                case "openai" -> openAIService != null ? openAIService.analyzeError(prompt).block() : null;
//                case "gemini" -> geminiService != null ? geminiService.analyzeError(prompt) : null;
//                default -> {
//                    System.out.println(ConsoleColors.YELLOW + "⚠️ Provider " + provider + " tidak dikenal, pakai Groq" + ConsoleColors.RESET);
//                    yield groqService.analyzeError(prompt).block();
//                }
//            };
//        } catch (Exception e) {
//            String errorMsg = e.getMessage();
//            if (errorMsg != null && errorMsg.contains("api_key")) {
//                errorMsg = errorMsg.replaceAll("api_key=[^&\\s]+", "api_key=***MASKED***");
//            }
//            System.err.println(ConsoleColors.RED + "❌ Error panggil AI: " + errorMsg + ConsoleColors.RESET);
//
//            return null;
//        }
//    }

    // TODO: Promt B Indo
    private String buildPrompt(ErrorContext errorContext) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("""
            Anda adalah asisten debugging Java Spring Boot. Jawab dalam BAHASA INDONESIA.
            
            """);

        if (sessionEnabled) {
            String sessionContext = debugSession.getSessionContext(errorContext);
            if (!sessionContext.isEmpty()) {
                prompt.append(sessionContext);
            }
        }

        prompt.append(String.format("""
            ERROR: %s
            PESAN: %s
            LOKASI: %s.%s() baris %d
            
            KODE:
            %s
            
            Berikan:
            ANALISIS: (penyebab error, singkat)
            PERBAIKAN: (langkah-langkah)
            KODE: (contoh perbaikan - SATU LINE untuk auto-fix)
            KEYAKINAN: (HIGH/MEDIUM/LOW)
            """,
                errorContext.getExceptionType(),
                errorContext.getMessage(),
                errorContext.getClassName(),
                errorContext.getMethodName(),
                errorContext.getLineNumber(),
                errorContext.getSourceCode() != null ? errorContext.getSourceCode() : "Tidak tersedia"
        ));

        return prompt.toString();
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