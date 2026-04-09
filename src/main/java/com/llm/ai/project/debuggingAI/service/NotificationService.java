package com.llm.ai.project.debuggingAI.service;

import com.llm.ai.project.debuggingAI.model.AIDebugResponse;
import com.llm.ai.project.debuggingAI.model.ErrorContext;
import com.llm.ai.project.debuggingAI.util.ConsoleColors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class NotificationService {

    @Value("${discord.webhook.url:}")
    private String discordWebhook;

    @Value("${slack.webhook.url:}")
    private String slackWebhook;

    @Value("${notification.enabled:false}")
    private boolean notificationEnabled;

    private final WebClient webClient;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public NotificationService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public void sendErrorNotification(ErrorContext context, AIDebugResponse response, String provider) {
        System.out.println("📢 sendErrorNotification DIPANGGIL!");
        System.out.println("   notification.enabled = " + notificationEnabled);
        System.out.println("   discord.webhook.url = " + (discordWebhook != null && !discordWebhook.isEmpty() ? discordWebhook.substring(0, 50) + "..." : "KOSONG"));

        if (!notificationEnabled) {
            System.out.println(ConsoleColors.YELLOW + "🔕 Notifikasi dinonaktifkan" + ConsoleColors.RESET);
            return;
        }

        // Discord
        if (discordWebhook != null && !discordWebhook.isEmpty()) {  // ← FIX: discordWebhook.isEmpty()
            sendToDiscord(context, response, provider)
                    .doOnSuccess(v -> System.out.println(ConsoleColors.GREEN + "📤 Notifikasi terkirim ke Discord" + ConsoleColors.RESET))
                    .doOnError(e -> {
                        System.err.println("❌ Gagal kirim ke Discord: " + e.getMessage());
                        e.printStackTrace();  // ← Tambah stack trace untuk debugging
                    })
                    .subscribe();
        } else {
            System.out.println(ConsoleColors.YELLOW + "⚠️ Discord webhook tidak dikonfigurasi" + ConsoleColors.RESET);
        }

        // Slack
        if (slackWebhook != null && !slackWebhook.isEmpty()) {
            sendToSlack(context, response, provider)
                    .doOnSuccess(v -> System.out.println(ConsoleColors.GREEN + "📤 Notifikasi terkirim ke Slack" + ConsoleColors.RESET))
                    .doOnError(e -> System.err.println("❌ Gagal kirim ke Slack: " + e.getMessage()))
                    .subscribe();
        }

        if ((slackWebhook == null || slackWebhook.isEmpty()) &&
                (discordWebhook == null || discordWebhook.isEmpty())) {
            System.out.println(ConsoleColors.YELLOW + "⚠️ Tidak ada webhook yang dikonfigurasi (Slack/Discord)" + ConsoleColors.RESET);
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

        // Send to Discord
        if (discordWebhook != null && !discordWebhook.isEmpty()) {
            sendPlainMessage(discordWebhook, message).subscribe();
        }

        // Send to Slack
        if (slackWebhook != null && !slackWebhook.isEmpty()) {
            sendPlainMessage(slackWebhook, message).subscribe();
        }
    }

    private Mono<String> sendToDiscord(ErrorContext context, AIDebugResponse response, String provider) {
        // Tentukan warna embed berdasarkan tingkat keyakinan
        int color = switch(response.getConfidence()) {
            case "HIGH" -> 0x00FF00;   // Hijau
            case "MEDIUM" -> 0xFFA500; // Oranye
            default -> 0xFF0000;       // Merah
        };

        // Batasi panjang stack trace agar tidak memenuhi chat
        String errorMessage = context.getMessage() != null ? context.getMessage() : "No message";
        if (errorMessage.length() > 1000) {
            errorMessage = errorMessage.substring(0, 997) + "...";
        }

        // Siapkan payload sesuai format Discord Webhook
        Map<String, Object> discordPayload = Map.of(
                "username", "AI Debug Assistant",
                "avatar_url", "https://i.imgur.com/oBPXx0D.png",
                "embeds", List.of(
                        Map.of(
                                "title", String.format("🚨 %s", context.getExceptionType().substring(context.getExceptionType().lastIndexOf('.') + 1)),
                                "description", String.format("**Pesan:** %s", errorMessage),
                                "color", color,
                                "fields", List.of(
                                        Map.of(
                                                "name", "📍 Lokasi",
                                                "value", String.format("`%s.%s()` baris %d",
                                                        context.getClassName(),
                                                        context.getMethodName(),
                                                        context.getLineNumber()),
                                                "inline", true
                                        ),
                                        Map.of(
                                                "name", "🤖 Provider",
                                                "value", provider.toUpperCase(),
                                                "inline", true
                                        ),
                                        Map.of(
                                                "name", "💡 Saran Perbaikan",
                                                "value", response.getSuggestedFix() != null ?
                                                        (response.getSuggestedFix().length() > 1024 ?
                                                                response.getSuggestedFix().substring(0, 1021) + "..." :
                                                                response.getSuggestedFix()) :
                                                        "Tidak ada saran"
                                        )
                                ),
                                "footer", Map.of(
                                        "text", String.format("AI Debug Assistant • %s • Keyakinan: %s",
                                                LocalDateTime.now().format(TIME_FORMAT),
                                                response.getConfidence())
                                )
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

    private Mono<String> sendToSlack(ErrorContext context, AIDebugResponse response, String provider) {
        // ... kode Slack yang sudah ada sebelumnya ...
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
                            "text": "🚨 ERROR DETECTED: %s",
                            "emoji": true
                        }
                    },
                    {
                        "type": "section",
                        "fields": [
                            {
                                "type": "mrkdwn",
                                "text": "*📍 Location:*\\n`%s.%s()` line %d"
                            },
                            {
                                "type": "mrkdwn",
                                "text": "*🤖 Provider:*\\n%s %s"
                            }
                        ]
                    },
                    {
                        "type": "section",
                        "text": {
                            "type": "mrkdwn",
                            "text": "*❌ Error Message:*\\n```%s```"
                        }
                    },
                    {
                        "type": "section",
                        "text": {
                            "type": "mrkdwn",
                            "text": "*💡 AI Suggestion:*\\n%s"
                        }
                    },
                    {
                        "type": "divider"
                    },
                    {
                        "type": "context",
                        "elements": [
                            {
                                "type": "plain_text",
                                "text": "%s | Confidence: %s",
                                "emoji": true
                            }
                        ]
                    }
                ]
            }
            """,
                context.getExceptionType().substring(context.getExceptionType().lastIndexOf('.') + 1),
                context.getClassName(),
                context.getMethodName(),
                context.getLineNumber(),
                provider.toUpperCase(),
                confidenceEmoji,
                context.getMessage() != null ? context.getMessage() : "No message",
                response.getSuggestedFix() != null ?
                        (response.getSuggestedFix().length() > 500 ?
                                response.getSuggestedFix().substring(0, 497) + "..." :
                                response.getSuggestedFix()) :
                        "No suggestion",
                LocalDateTime.now().format(TIME_FORMAT),
                response.getConfidence()
        );

        return webClient.post()
                .uri(slackWebhook)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(message)
                .retrieve()
                .bodyToMono(String.class);
    }

    private Mono<String> sendPlainMessage(String webhookUrl, String message) {
        // Format sederhana untuk Discord plain text (fallback)
        Map<String, String> payload = Map.of("content", message);

        return webClient.post()
                .uri(webhookUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class);
    }
}
