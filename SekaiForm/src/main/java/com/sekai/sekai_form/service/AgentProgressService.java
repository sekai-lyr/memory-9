package com.sekai.sekai_form.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AgentProgressService {
    private final Map<String, Snapshot> progress = new ConcurrentHashMap<>();

    public void publish(String conversationId, String phase, String message) {
        if (conversationId == null || conversationId.isBlank()) return;
        progress.put(conversationId, new Snapshot(phase, message, Instant.now()));
    }

    public Snapshot get(String conversationId) { return progress.get(conversationId); }

    public record Snapshot(String phase, String message, Instant updatedAt) { }
}
