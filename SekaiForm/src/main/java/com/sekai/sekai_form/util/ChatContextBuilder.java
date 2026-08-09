package com.sekai.sekai_form.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * 7.24.pm Chat Context Builder - Simplified 3-layer system.
 */
public class ChatContextBuilder {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static ArrayNode buildMessages(
            String systemPrompt,
            String apiModel,
            String message,
            List<Map<String, String>> history) {
        
        var h = LocalTime.now().getHour();
        var dow = LocalDate.now().getDayOfWeek().toString();
        
        ArrayNode messages = mapper.createArrayNode();
        
        // Layer 1: System prompt with time awareness
        String timeCtx = dow + " " + h + ":00. ";
        if (h >= 5 && h < 9) timeCtx += "Morning - be fresh and encouraging.";
        else if (h >= 12 && h < 14) timeCtx += "Lunch time - light and warm.";
        else if (h >= 18 && h < 21) timeCtx += "Evening - user may be tired, be extra warm.";
        else if (h >= 21 || h < 5) timeCtx += "Late night - gentle, quiet companionship.";
        else timeCtx += "Be naturally warm.";
        
        String fullPrompt = systemPrompt + "\n\n[Time: " + timeCtx + "]";
        messages.add(msg("system", fullPrompt));
        
        // Layer 2: Emotion detection
        if (message != null) {
            String emotion = null;
            if (message.contains("累") || message.contains("辛苦") || message.contains("疲惫"))
                emotion = "User feels tired. Give warm, specific comfort. Don't just say 'rest well' - be concrete.";
            else if (message.contains("开心") || message.contains("高兴") || message.contains("快乐"))
                emotion = "User is happy. Celebrate with them, ask for details to amplify their joy.";
            else if (message.contains("难过") || message.contains("伤心") || message.contains("哭"))
                emotion = "User is sad. Empathize deeply first, then gently guide. Don't rush to solutions.";
            else if (message.contains("无聊") || message.contains("没意思"))
                emotion = "User is bored. Engage creatively - suggest a topic, game, or story.";
            if (emotion != null) messages.add(msg("system", "[Emotion] " + emotion));
        }
        
        // Layer 3: Recent history (last 10 messages)
        if (history != null && !history.isEmpty()) {
            int start = Math.max(0, history.size() - 10);
            for (int i = start; i < history.size(); i++) {
                var hm = history.get(i);
                String role = hm.getOrDefault("role", "user");
                String content = hm.getOrDefault("content", "");
                if (!content.isBlank()) {
                    messages.add(msg(role, content));
                }
            }
        }
        
        // Current message
        if (message != null && !message.isBlank()) {
            messages.add(msg("user", message));
        }
        
        return messages;
    }

    private static ObjectNode msg(String role, String content) {
        ObjectNode node = mapper.createObjectNode();
        node.put("role", role);
        node.put("content", content);
        return node;
    }
}