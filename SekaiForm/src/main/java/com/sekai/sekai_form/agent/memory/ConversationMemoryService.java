package com.sekai.sekai_form.agent.memory;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ConversationMemoryService {
    private static final int RECENT_MESSAGES = 8;
    private static final int SUMMARY_TRIGGER = 12;
    private static final int SUMMARY_MAX_CHARS = 4000;
    private final JdbcTemplate jdbc;

    public ConversationMemoryService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public MemoryContext load(String conversationId, String userId, String state) {
        ensureConversation(conversationId, userId, null, state);
        String summary = jdbc.query("SELECT rolling_summary FROM agent_conversation WHERE conversation_id = ?",
                rs -> rs.next() ? rs.getString(1) : "", conversationId);
        List<MemoryMessage> messages = jdbc.query(
                "SELECT role, content, modality FROM agent_message WHERE conversation_id = ? ORDER BY id DESC LIMIT ?",
                (rs, row) -> new MemoryMessage(rs.getString("role"), rs.getString("content"), rs.getString("modality")),
                conversationId, RECENT_MESSAGES);
        java.util.Collections.reverse(messages);
        return new MemoryContext(summary == null ? "" : summary, messages, state == null ? "active" : state);
    }

    public List<MemoryMessage> recent(String conversationId) {
        return load(conversationId, "anonymous", "active").recentMessages();
    }

    public void saveTurn(String conversationId, String userId, Long modelId, String modality,
                         String userMessage, String assistantMessage) {
        ensureConversation(conversationId, userId, modelId, "active");
        insertMessage(conversationId, "user", userMessage, modality);
        insertMessage(conversationId, "assistant", assistantMessage, modality);
        jdbc.update("UPDATE agent_conversation SET last_activity = CURRENT_TIMESTAMP WHERE conversation_id = ?", conversationId);
        rollSummaryIfNeeded(conversationId);
    }

    public void saveMemory(String conversationId, String userId, String content, String type, double importance) {
        if (content == null || content.isBlank()) return;
        jdbc.update("INSERT INTO agent_memory(conversation_id,user_id,memory_type,content,importance) VALUES(?,?,?,?,?)",
                conversationId, userId, type == null ? "conversation" : type, limit(content, 6000), importance);
    }

    public List<String> relevantMemories(String conversationId, String query, int limit) {
        List<String> all = jdbc.query("SELECT content FROM agent_memory WHERE conversation_id = ? ORDER BY importance DESC, created_at DESC",
                (rs, row) -> rs.getString(1), conversationId);
        String q = query == null ? "" : query.toLowerCase();
        List<String> result = new ArrayList<>();
        for (String item : all) {
            if (result.size() >= Math.max(1, limit)) break;
            if (q.isBlank() || item.toLowerCase().contains(q) || overlap(item, q) > 0) result.add(item);
        }
        return result;
    }

    private void ensureConversation(String id, String userId, Long modelId, String state) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("conversationId is required");
        int updated = jdbc.update("UPDATE agent_conversation SET last_activity = CURRENT_TIMESTAMP WHERE conversation_id = ?", id);
        if (updated == 0) {
            jdbc.update("INSERT INTO agent_conversation(conversation_id,user_id,model_id,session_state) VALUES(?,?,?,?)",
                    id, userId, modelId, state == null ? "active" : state);
        }
    }

    private void insertMessage(String conversationId, String role, String content, String modality) {
        jdbc.update("INSERT INTO agent_message(conversation_id,role,content,modality) VALUES(?,?,?,?)",
                conversationId, role, limit(content == null ? "" : content, 12000), modality == null ? "TEXT" : modality);
    }

    private void rollSummaryIfNeeded(String conversationId) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM agent_message WHERE conversation_id = ?", Integer.class, conversationId);
        if (count == null || count <= SUMMARY_TRIGGER) return;
        List<MemoryMessage> all = jdbc.query("SELECT role, content, modality FROM agent_message WHERE conversation_id = ? ORDER BY id",
                (rs, row) -> new MemoryMessage(rs.getString("role"), rs.getString("content"), rs.getString("modality")), conversationId);
        int oldCount = Math.max(0, all.size() - RECENT_MESSAGES);
        if (oldCount == 0) return;
        String prior = jdbc.query("SELECT rolling_summary FROM agent_conversation WHERE conversation_id = ?",
                rs -> rs.next() ? rs.getString(1) : "", conversationId);
        StringBuilder summary = new StringBuilder();
        if (prior != null && !prior.isBlank()) summary.append(prior).append("\n");
        summary.append("此前对话要点：");
        for (int i = 0; i < oldCount; i++) {
            MemoryMessage message = all.get(i);
            String content = limit(message.content(), 220);
            if (!content.isBlank()) summary.append(" ").append(message.role()).append("：").append(content);
        }
        jdbc.update("UPDATE agent_conversation SET rolling_summary = ?, summary_message_count = ? WHERE conversation_id = ?",
                limit(summary.toString(), SUMMARY_MAX_CHARS), count, conversationId);
    }

    private static int overlap(String text, String query) {
        int score = 0;
        for (String token : query.split("\\s+")) if (!token.isBlank() && text.toLowerCase().contains(token)) score++;
        return score;
    }

    private static String limit(String value, int max) {
        if (value == null) return "";
        return value.length() <= max ? value : value.substring(0, max) + "…";
    }
}
