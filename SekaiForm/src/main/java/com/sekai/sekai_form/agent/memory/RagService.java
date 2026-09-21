package com.sekai.sekai_form.agent.memory;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class RagService {
    private final JdbcTemplate jdbc;

    public RagService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public void save(String conversationId, String userId, String title, String content, String metadata) {
        if (content == null || content.isBlank()) return;
        jdbc.update("INSERT INTO agent_rag_document(conversation_id,user_id,title,content,metadata) VALUES(?,?,?,?,?)",
                conversationId, userId, limit(title, 300), limit(content, 12000), limit(metadata, 2000));
    }

    public List<RagHit> search(String conversationId, String userId, String query, int limit) {
        List<RagHit> candidates = jdbc.query(
                "SELECT title, content FROM agent_rag_document WHERE (conversation_id = ? OR user_id = ?) ORDER BY created_at DESC",
                (rs, row) -> new RagHit(rs.getString("title"), rs.getString("content"), score(rs.getString("content"), query)),
                conversationId, userId);
        candidates.sort(Comparator.comparingDouble(RagHit::score).reversed());
        List<RagHit> result = new ArrayList<>();
        for (RagHit hit : candidates) if (hit.score() > 0 && result.size() < Math.max(1, limit)) result.add(hit);
        return result;
    }

    private static double score(String content, String query) {
        if (query == null || query.isBlank() || content == null) return 0;
        String text = content.toLowerCase();
        String q = query.toLowerCase();
        double value = text.contains(q) ? 2.0 : 0;
        for (String token : tokens(q)) if (text.contains(token)) value += token.length() > 1 ? 1.0 : 0.2;
        return value;
    }

    private static List<String> tokens(String text) {
        List<String> values = new ArrayList<>();
        for (String token : text.split("[^\\p{L}\\p{N}\\u4e00-\\u9fff]+")) {
            if (token.length() > 1) values.add(token);
            for (int i = 0; i < token.length(); i++) {
                char ch = token.charAt(i);
                if (ch >= '\u4e00' && ch <= '\u9fff') values.add(String.valueOf(ch));
            }
        }
        return values;
    }

    private static String limit(String value, int max) { return value == null ? "" : value.length() <= max ? value : value.substring(0, max) + "…"; }
}
