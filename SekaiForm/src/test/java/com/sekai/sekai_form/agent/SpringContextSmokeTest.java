package com.sekai.sekai_form.agent;

import com.sekai.sekai_form.tool.ToolRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.main.web-application-type=none",
        "spring.datasource.url=jdbc:h2:mem:agent_context;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:schema.sql"
})
class SpringContextSmokeTest {
    @Autowired AgentService agentService;
    @Autowired ToolRegistry toolRegistry;
    @Autowired JdbcTemplate jdbcTemplate;

    @Test void completeAgentRuntimeWiresIntoApplicationContext() {
        assertTrue(agentService != null);
        assertTrue(toolRegistry.size() >= 9);
        assertTrue(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM live2d_chat_config WHERE model_id=1", Integer.class) >= 1);
        String apiUrl = jdbcTemplate.queryForObject("SELECT api_url FROM live2d_chat_config WHERE model_id=1", String.class);
        assertTrue(apiUrl.endsWith("/chat/completions"));
    }
}
