package com.sekai.sekai_form.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sekai.sekai_form.dataobject.Live2DChatConfigDO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.main.web-application-type=none",
        "spring.datasource.url=jdbc:h2:mem:agent_context_memory;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:schema.sql"
})
@Import(AgentServiceContextTest.FakeModelConfiguration.class)
class AgentServiceContextTest {
    @Autowired AgentService agentService;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach void enableTestModelConfig() {
        jdbc.update("UPDATE live2d_chat_config SET api_key = ? WHERE model_id = ?", "test-key", 1L);
    }

    @Test void laterTurnReceivesEarlierConversationForReferenceResolution() {
        AgentResult first = agentService.run(AgentRequest.builder().modelId(1L)
                .conversationId("context-reference").userId("test-user")
                .message("我喜欢草莓牛奶，请记住。").build());
        AgentResult second = agentService.run(AgentRequest.builder().modelId(1L)
                .conversationId("context-reference").userId("test-user")
                .message("我喜欢什么？").build());
        assertEquals("已记住你的偏好。", first.getReply());
        assertEquals("我记得你喜欢草莓牛奶。", second.getReply());
    }

    @TestConfiguration
    static class FakeModelConfiguration {
        @Bean
        @Primary
        AgentModelClient fakeModel(ObjectMapper mapper) {
            return new AgentModelClient() {
                @Override public JsonNode complete(Live2DChatConfigDO config, ArrayNode messages, ArrayNode tools) {
                    boolean containsPreference = messages.size() > 2;
                    ObjectNode answer = mapper.createObjectNode();
                    answer.put("role", "assistant");
                    answer.put("content", containsPreference ? "我记得你喜欢草莓牛奶。" : "已记住你的偏好。");
                    ObjectNode choice = mapper.createObjectNode();
                    choice.set("message", answer);
                    return mapper.createObjectNode().set("choices", mapper.createArrayNode().add(choice));
                }
            };
        }
    }
}
