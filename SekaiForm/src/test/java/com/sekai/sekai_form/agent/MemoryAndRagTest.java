package com.sekai.sekai_form.agent;

import com.sekai.sekai_form.agent.memory.ConversationMemoryService;
import com.sekai.sekai_form.agent.memory.MemoryContext;
import com.sekai.sekai_form.agent.memory.RagHit;
import com.sekai.sekai_form.agent.memory.RagService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@JdbcTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:agent_test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:schema.sql"
})
class MemoryAndRagTest {
    @Autowired JdbcTemplate jdbc;

    @Test void conversationMemoryRollsOlderMessagesIntoSummary() {
        ConversationMemoryService memory = new ConversationMemoryService(jdbc);
        for (int i = 0; i < 7; i++) memory.saveTurn("conversation-1", "user-1", 1L, "TEXT", "用户偏好第" + i + "项", "角色记住第" + i + "项");
        MemoryContext context = memory.load("conversation-1", "user-1", "active");
        assertFalse(context.summary().isBlank());
        assertEquals(8, context.recentMessages().size());
        assertTrue(context.summary().contains("用户偏好第"));
    }

    @Test void ragDocumentsCanBeSavedAndRetrievedByConversation() {
        RagService rag = new RagService(jdbc);
        rag.save("conversation-2", "user-2", "角色设定", "Haru 喜欢草莓牛奶，Live2D 角色会在下午提醒休息。", "domain=live2d");
        List<RagHit> hits = rag.search("conversation-2", "user-2", "草莓牛奶", 3);
        assertEquals(1, hits.size());
        assertTrue(hits.get(0).content().contains("草莓牛奶"));
    }
}
