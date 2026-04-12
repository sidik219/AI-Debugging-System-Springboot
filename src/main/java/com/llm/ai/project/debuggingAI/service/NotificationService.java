package com.llm.ai.project.debuggingAI.service;

import com.llm.ai.project.debuggingAI.model.AIDebugResponse;
import com.llm.ai.project.debuggingAI.model.ErrorContext;
import com.llm.ai.project.debuggingAI.util.ConsoleColors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class NotificationService {

    @Value("${discord.webhook.url:}")
    private String discordWebhook;

    @Value("${slack.webhook.url:}")
    private String slackWebhook;

    @Value("${telegram.bot.token:}")
    private String telegramBotToken;

    @Value("${telegram.chat.id:}")
    private String telegramChatId;

    @Value("${notification.enabled:false}")
    private boolean notificationEnabled;

    @Value("${notification.summary.enabled:false}")
    private boolean summaryEnabled;

    @Value("${notification.summary.cron:0 0 * * * *}")
    private String summaryCron;

    @Value("${notification.rate-limit-seconds:60}")
    private int rateLimitSeconds;

    private final WebClient webClient;
    private final Map<String, LocalDateTime> lastNotificationTime = new ConcurrentHashMap<>();
    private final Map<String, ErrorSummary> errorSummaries = new ConcurrentHashMap<>();
    private LocalDateTime lastSummaryTime = LocalDateTime.now();
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public NotificationService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    private boolean isRateLimited(String errorSignature) {
        LocalDateTime lastTime = lastNotificationTime.get(errorSignature);
        if (lastTime == null) return false;

        LocalDateTime now = LocalDateTime.now();
        long secondsSinceLast = java.time.Duration.between(lastTime, now).getSeconds();

        return secondsSinceLast < rateLimitSeconds;
    }

    private void updateLastNotificationTime(String errorSignature) {
        lastNotificationTime.put(errorSignature, LocalDateTime.now());
    }

    private String generateErrorSignature(ErrorContext context) {
        return context.getExceptionType() + ":" +
                context.getClassName() + "." +
                context.getMethodName() + ":" +
                context.getLineNumber();
    }

    public void sendErrorNotification(ErrorContext context, AIDebugResponse response, String provider) {
        if (!notificationEnabled) {
            System.out.println(ConsoleColors.YELLOW + "🔕 Notifikasi dinonaktifkan" + ConsoleColors.RESET);
            return;
        }

        String errorSignature = generateErrorSignature(context);

        // Rate Limit
        if (isRateLimited(errorSignature)) {
            System.out.println(ConsoleColors.YELLOW + "⏳ Rate limited: Notifikasi untuk error ini sudah dikirim dalam " +
                    rateLimitSeconds + " detik terakhir." + ConsoleColors.RESET);
            return;
        }

        System.out.println(ConsoleColors.CYAN + "📢 Mengirim notifikasi..." + ConsoleColors.RESET);

        // Discord
        if (discordWebhook != null && !discordWebhook.isEmpty()) {
            sendToDiscord(context, response, provider)
                    .doOnSuccess(v -> System.out.println(ConsoleColors.GREEN + "📤 Notifikasi terkirim ke Discord" + ConsoleColors.RESET))
                    .doOnError(e -> System.err.println("❌ Gagal kirim ke Discord: " + e.getMessage()))
                    .subscribe();
        }

        // Slack
        if (slackWebhook != null && !slackWebhook.isEmpty()) {
            sendToSlack(context, response, provider)
                    .doOnSuccess(v -> System.out.println(ConsoleColors.GREEN + "📤 Notifikasi terkirim ke Slack" + ConsoleColors.RESET))
                    .doOnError(e -> System.err.println("❌ Gagal kirim ke Slack: " + e.getMessage()))
                    .subscribe();
        }

        // Telegram
        if (telegramBotToken != null && !telegramBotToken.isEmpty() &&
                telegramChatId != null && !telegramChatId.isEmpty()) {
            sendToTelegram(context, response, provider)
                    .doOnSuccess(v -> System.out.println(ConsoleColors.GREEN + "📤 Notifikasi terkirim ke Telegram" + ConsoleColors.RESET))
                    .doOnError(e -> System.err.println("❌ Gagal kirim ke Telegram: " + e.getMessage()))
                    .subscribe();
        }

        // Update last notification time
        updateLastNotificationTime(errorSignature);

        if ((discordWebhook == null || discordWebhook.isEmpty()) &&
                (slackWebhook == null || slackWebhook.isEmpty()) &&
                (telegramBotToken == null || telegramBotToken.isEmpty())) {
            System.out.println(ConsoleColors.YELLOW + "⚠️ Tidak ada webhook yang dikonfigurasi (Discord/Slack/Telegram)" + ConsoleColors.RESET);
        }
    }

    public void sendSuccessNotification(ErrorContext context, int attempts) {
        if (!notificationEnabled) return;

        String message = String.format("✅ **ERROR FIXED!**\n" +
                        "`%s.%s()` telah diperbaiki setelah *%d* percobaan.\n" +
                        "_%s_",
                context.getClassName(),
                context.getMethodName(),
                attempts,
                LocalDateTime.now().format(TIME_FORMAT));

        // Discord
        if (discordWebhook != null && !discordWebhook.isEmpty()) {
            sendPlainMessage(discordWebhook, message).subscribe();
        }

        // Slack
        if (slackWebhook != null && !slackWebhook.isEmpty()) {
            sendPlainMessage(slackWebhook, message).subscribe();
        }

        // Telegram
        if (telegramBotToken != null && !telegramBotToken.isEmpty() &&
                telegramChatId != null && !telegramChatId.isEmpty()) {
            sendTelegramMessage(message).subscribe();
        }
    }

    // TODO: ==================== DISCORD ====================

    private Mono<String> sendToDiscord(ErrorContext context, AIDebugResponse response, String provider) {
        int color = switch(response.getConfidence()) {
            case "HIGH" -> 0x00FF00;
            case "MEDIUM" -> 0xFFA500;
            default -> 0xFF0000;
        };

        String errorMessage = context.getMessage() != null ? context.getMessage() : "No message";
        if (errorMessage.length() > 1000) {
            errorMessage = errorMessage.substring(0, 997) + "...";
        }

        Map<String, Object> discordPayload = Map.of(
                "username", "AI Debug Assistant",
                "embeds", List.of(
                        Map.of(
                                "title", String.format("🚨 %s", context.getExceptionType().substring(context.getExceptionType().lastIndexOf('.') + 1)),
                                "description", String.format("**Pesan:** %s", errorMessage),
                                "color", color,
                                "fields", List.of(
                                        Map.of("name", "📍 Lokasi", "value",
                                                String.format("`%s.%s()` baris %d", context.getClassName(), context.getMethodName(), context.getLineNumber()),
                                                "inline", true),
                                        Map.of("name", "🤖 Provider", "value", provider.toUpperCase(), "inline", true),
                                        Map.of("name", "💡 Saran", "value",
                                                response.getSuggestedFix() != null ?
                                                        (response.getSuggestedFix().length() > 1024 ?
                                                                response.getSuggestedFix().substring(0, 1021) + "..." :
                                                                response.getSuggestedFix()) :
                                                        "Tidak ada saran")
                                ),
                                "footer", Map.of("text", String.format("AI Debug • %s • Keyakinan: %s",
                                        LocalDateTime.now().format(TIME_FORMAT), response.getConfidence()))
                        )
                )
        );

        return webClient.post()
                .uri(discordWebhook)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(discordPayload)
                .retrieve()
                .bodyToMono(String.class);
    }

    // TODO: ==================== SLACK ====================

    private Mono<String> sendToSlack(ErrorContext context, AIDebugResponse response, String provider) {
        String confidenceEmoji = switch(response.getConfidence()) {
            case "HIGH" -> "🟢";
            case "MEDIUM" -> "🟡";
            default -> "🔴";
        };

        String message = String.format("""
            {
                "blocks": [
                    {
                        "type": "header",
                        "text": {
                            "type": "plain_text",
                            "text": "🚨 ERROR: %s",
                            "emoji": true
                        }
                    },
                    {
                        "type": "section",
                        "fields": [
                            {"type": "mrkdwn", "text": "*📍 Location:*\\n`%s.%s()` line %d"},
                            {"type": "mrkdwn", "text": "*🤖 Provider:*\\n%s %s"}
                        ]
                    },
                    {
                        "type": "section",
                        "text": {"type": "mrkdwn", "text": "*💡 Suggestion:*\\n%s"}
                    },
                    {
                        "type": "context",
                        "elements": [{"type": "plain_text", "text": "%s | Confidence: %s", "emoji": true}]
                    }
                ]
            }
            """,
                context.getExceptionType().substring(context.getExceptionType().lastIndexOf('.') + 1),
                context.getClassName(), context.getMethodName(), context.getLineNumber(),
                provider.toUpperCase(), confidenceEmoji,
                response.getSuggestedFix() != null ? response.getSuggestedFix() : "No suggestion",
                LocalDateTime.now().format(TIME_FORMAT), response.getConfidence()
        );

        return webClient.post()
                .uri(slackWebhook)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(message)
                .retrieve()
                .bodyToMono(String.class);
    }

    // TODO: ==================== TELEGRAM ====================

    private Mono<String> sendToTelegram(ErrorContext context, AIDebugResponse response, String provider) {
        String confidenceEmoji = switch(response.getConfidence()) {
            case "HIGH" -> "🟢";
            case "MEDIUM" -> "🟡";
            default -> "🔴";
        };

        String message = String.format(
                "🚨 *ERROR DETECTED!*\n\n" +
                        "*Tipe:* `%s`\n" +
                        "*Pesan:* %s\n" +
                        "*Lokasi:* `%s.%s()` baris %d\n" +
                        "*Provider:* %s %s\n\n" +
                        "💡 *Saran Perbaikan:*\n%s\n\n" +
                        "_%s | Keyakinan: %s_",
                context.getExceptionType().substring(context.getExceptionType().lastIndexOf('.') + 1),
                escapeTelegram(context.getMessage() != null ? context.getMessage() : "No message"),
                context.getClassName(), context.getMethodName(), context.getLineNumber(),
                provider.toUpperCase(), confidenceEmoji,
                escapeTelegram(response.getSuggestedFix() != null ? response.getSuggestedFix() : "Tidak ada saran"),
                LocalDateTime.now().format(TIME_FORMAT), response.getConfidence()
        );

        return sendTelegramMessage(message);
    }

    private Mono<String> sendTelegramMessage(String message) {
        String url = String.format("https://api.telegram.org/bot%s/sendMessage", telegramBotToken);

        Object chatIdValue;
        try {
            chatIdValue = Long.parseLong(telegramChatId);  // Private chat = angka positif
        } catch (NumberFormatException e) {
            chatIdValue = telegramChatId;  // Fallback ke String (untuk group/channel dengan "-")
        }

        Map<String, Object> payload = Map.of(
                "chat_id", chatIdValue,
                "text", message,
                "parse_mode", "Markdown",
                "disable_web_page_preview", true
        );

        return webClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class);
    }

    private String escapeTelegram(String text) {
        if (text == null) return "";
        return text.replace("_", "\\_")
                .replace("*", "\\*")
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("~", "\\~")
                .replace("`", "\\`")
                .replace(">", "\\>")
                .replace("#", "\\#")
                .replace("+", "\\+")
                .replace("-", "\\-")
                .replace("=", "\\=")
                .replace("|", "\\|")
                .replace("{", "\\{")
                .replace("}", "\\}")
                .replace(".", "\\.")
                .replace("!", "\\!");
    }

    // TODO: ==================== SUMMARY REPORT ====================

    public void recordErrorForSummary(ErrorContext context) {
        if (!summaryEnabled) return;

        String key = context.getExceptionType() + "." +
                context.getClassName() + "." +
                context.getMethodName();

        errorSummaries.compute(key, (k, v) -> {
           if (v == null) {
               return new ErrorSummary(context);
           }

           v.incrementCount();
           v.updateLastOccurrence();

           return v;
        });
    }

    public void sendSummaryReport() {
        if (!summaryEnabled || !notificationEnabled) return;
        if (errorSummaries.isEmpty()) return;

        LocalDateTime now = LocalDateTime.now();
        long hours = Duration.between(lastSummaryTime, now).toHours();
        lastSummaryTime = now;

        int totalErrors = errorSummaries.values().stream().mapToInt(ErrorSummary::getCount).sum();

        StringBuilder sb = new StringBuilder();
        sb.append("📊 **AI DEBUG SUMMARY** (Last ").append(hours).append(" hours)\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("Total Errors: **").append(totalErrors).append("**\n\n");
        sb.append("🔥 **Most Common Errors:**\n");

        errorSummaries
                .entrySet()
                .stream()
                .sorted((a, b) -> Integer.compare(b.getValue().getCount(), a.getValue().getCount()))
                .limit(5)
                .forEach(entry -> {
                    ErrorSummary es = entry.getValue();
                    sb.append("• `")
                            .append(es.getExceptionType()).append("` at `")
                            .append(es.getMethodLocation()).append("` - ")
                            .append(es.getCount()).append("x\n");
                });

        String message = sb.toString();
        if (discordWebhook != null && !discordWebhook.isEmpty()) {
            sendSummaryToDiscord(message).subscribe();
        }

        if (telegramBotToken != null && !telegramBotToken.isEmpty()) {
            sendTelegramMessage(message).subscribe();
        }

        errorSummaries.clear();

        System.out.println(ConsoleColors.GREEN + "📊 Summary report sent!" + ConsoleColors.RESET);
    }

    private Mono<String> sendSummaryToDiscord(String message) {
        Map<String, Object> payload = Map.of(
                "username", "AI Debug Summary",
                "content", message
        );

        return webClient.post()
                .uri(discordWebhook)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class);
    }

    public Map<String, Object> getSummaryStats() {
        return Map.of(
                "enabled", summaryEnabled,
                "pendingErrors", errorSummaries.size(),
                "lastSummaryTime", lastSummaryTime.toString()
        );
    }

    public void clearSummaries() {
        errorSummaries.clear();
        System.out.println("🧹 Summaries cleared");
    }

    private static class ErrorSummary{
        private final String exceptionType;
        private final String methodLocation;
        private int count;
        private LocalDateTime lastOccurrence;

        public ErrorSummary(ErrorContext context) {
            this.exceptionType = context.getExceptionType()
                    .substring(context.getExceptionType().lastIndexOf('.') + 1);
            this.methodLocation = context.getClassName().substring(
                    context.getClassName().lastIndexOf('.') + 1
            ) + "." + context.getMethodName() + "()";
            this.count = 1;
            this.lastOccurrence = LocalDateTime.now();
        }

        public void incrementCount() { this.count++; }
        public void updateLastOccurrence() { this.lastOccurrence = LocalDateTime.now(); }
        public String getExceptionType() { return exceptionType; }
        public String getMethodLocation() { return methodLocation; }
        public int getCount() { return count; }
    }

    // TODO: ==================== PLAIN MESSAGE ====================

    private Mono<String> sendPlainMessage(String webhookUrl, String message) {
        Map<String, String> payload = Map.of("content", message);

        return webClient.post()
                .uri(webhookUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class);
    }

    // TODO: ==================== RATE LIMIT ADMIN ====================

    public void clearRateLimitCache() {
        lastNotificationTime.clear();
        System.out.println(ConsoleColors.GREEN + "✅ Rate limit cache cleared" + ConsoleColors.RESET);
    }

    public Map<String, Object> getRateLimitStatus() {
        return Map.of(
                "rateLimitSeconds", rateLimitSeconds,
                "cachedErrors", lastNotificationTime.size(),
                "cache", lastNotificationTime
        );
    }
}