package com.sekai.sekai_form.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sekai.sekai_form.dataobject.Live2DDialogueDO;
import com.sekai.sekai_form.mapper.Live2DDialogueMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
public class Live2DWebSocketHandler extends TextWebSocketHandler {
    private final Set<WebSocketSession> sessions = new CopyOnWriteArraySet<>();
    @Autowired(required = false)
    private Live2DDialogueMapper dialogueMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) { sessions.add(session); }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) { sessions.remove(session); }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        Map<String, Object> msg = objectMapper.readValue(payload, Map.class);
        String type = (String) msg.get("type");
        if ("poke".equals(type)) handlePoke(session, msg);
    }

    private void handlePoke(WebSocketSession session, Map<String, Object> msg) throws IOException {
        Long modelId = msg.get("modelId") != null ? ((Number) msg.get("modelId")).longValue() : null;
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("type", "poke_reply"); response.put("text", "Hey! Don't poke me~");
        if (modelId != null && dialogueMapper != null) {
            try {
                List<Live2DDialogueDO> dialogues = dialogueMapper.listByCategory(modelId, "click");
                if (dialogues != null && !dialogues.isEmpty()) {
                    Live2DDialogueDO d = dialogues.get(new Random().nextInt(dialogues.size()));
                    response.put("text", d.getText()); response.put("motion", d.getMotionName()); response.put("expression", d.getExpressionName());
                }
            } catch (Exception ignored) {}
        }
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
    }

    public void broadcast(Map<String, Object> data) throws IOException {
        String json = objectMapper.writeValueAsString(data);
        for (WebSocketSession s : sessions) { if (s.isOpen()) s.sendMessage(new TextMessage(json)); }
    }
}