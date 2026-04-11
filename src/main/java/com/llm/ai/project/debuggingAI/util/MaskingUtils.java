package com.llm.ai.project.debuggingAI.util;

public class MaskingUtils {

    public static String maskApiKey(String key) {
        if (key == null || key.isEmpty()) {
            return "NOT_SET";
        }

        if (key.length() <= 8) {
            return "***";
        }

        String prefix = key.substring(0, Math.min(4, key.length()));
        String suffix = key.substring(Math.max(key.length() - 4, 0));

        return prefix + "***" + suffix;
    }

    public static String maskWebhookUrl(String url) {
        if (url == null || url.isEmpty()) {
            return "NOT_SET";
        }

        int webhookIndex = url.indexOf("webhooks/");
        if (webhookIndex > 0) {
            String base = url.substring(0, webhookIndex + 9);
            String rest = url.substring(webhookIndex + 9);

            int slashIndex = rest.indexOf('/');
            if (slashIndex > 0) {
                String id = rest.substring(0, slashIndex);
                String token = rest.substring(slashIndex);
                return base + maskApiKey(id) + token;
            }
        }

        return maskApiKey(url);
    }

    public static String maskForLog(String key, String type) {
        if (key == null || key.isEmpty()) {
            return "[" + type + ": NOT_SET]";
        }

        return "[" + type + ": " + maskApiKey(key) + "]";
    }
}
