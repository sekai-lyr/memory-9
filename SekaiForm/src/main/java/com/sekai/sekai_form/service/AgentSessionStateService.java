package com.sekai.sekai_form.service;

import com.sekai.sekai_form.agent.AgentAttachment;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AgentSessionStateService {
    private static final long TTL_SECONDS = 7200;
    private final Map<String, SessionState> states = new ConcurrentHashMap<>();

    public void touch(String conversationId, String state) {
        if (conversationId == null || conversationId.isBlank()) return;
        states.compute(conversationId, (key, old) -> {
            SessionState value = old == null ? new SessionState() : old;
            value.lastActivity = Instant.now();
            value.state = state == null ? "active" : state;
            return value;
        });
    }

    public void putAttachment(String conversationId, AgentAttachment attachment) {
        if (attachment == null) return;
        SessionState value = states.computeIfAbsent(conversationId, key -> new SessionState());
        value.lastAttachment = attachment;
        value.lastActivity = Instant.now();
    }

    public AgentAttachment getLastAttachment(String conversationId) {
        SessionState value = states.get(conversationId);
        if (value == null || expired(value)) { states.remove(conversationId); return null; }
        return value.lastAttachment;
    }

    public String describe(String conversationId) {
        SessionState value = states.get(conversationId);
        if (value == null || expired(value)) return "active";
        return value.state + (value.lastAttachment == null ? "" : ",已有最近附件");
    }

    private boolean expired(SessionState value) { return value.lastActivity.plusSeconds(TTL_SECONDS).isBefore(Instant.now()); }
    private static final class SessionState {
        private Instant lastActivity = Instant.now();
        private String state = "active";
        private AgentAttachment lastAttachment;
    }
}
