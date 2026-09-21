package com.sekai.sekai_form.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.sekai.sekai_form.dataobject.Live2DChatConfigDO;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.POST;

class OpenAiCompatibleAgentModelClientTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void normalizesReferenceProjectEndpointAndBearerHeader() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo("https://api.example.test/v1/chat/completions"))
                .andExpect(method(POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-key"))
                .andExpect(content().json("{\"model\":\"deepseek-chat\",\"messages\":[{\"role\":\"user\",\"content\":\"hello\"}]}", false))
                .andRespond(withSuccess("{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"ok\"}}]}", MediaType.APPLICATION_JSON));

        Live2DChatConfigDO config = config("https://api.example.test/v1/", "Bearer test-key");
        ArrayNode messages = mapper.createArrayNode();
        messages.add(mapper.createObjectNode().put("role", "user").put("content", "hello"));

        assertEquals("ok", new OpenAiCompatibleAgentModelClient(mapper, restTemplate)
                .complete(config, messages, mapper.createArrayNode()).path("choices").path(0)
                .path("message").path("content").asText());
        server.verify();
    }

    @Test
    void mapsUnauthorizedResponseToSafeDiagnostic() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo("https://api.example.test/chat/completions"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        IllegalStateException error = assertThrows(IllegalStateException.class, () ->
                new OpenAiCompatibleAgentModelClient(mapper, restTemplate)
                        .complete(config("https://api.example.test", "test-key"),
                                mapper.createArrayNode(), mapper.createArrayNode()));

        assertEquals("模型鉴权失败，请检查 API Key。", error.getMessage());
        server.verify();
    }

    private Live2DChatConfigDO config(String url, String key) {
        Live2DChatConfigDO config = new Live2DChatConfigDO();
        config.setApiUrl(url);
        config.setApiKey(key);
        config.setModelName("deepseek-chat");
        return config;
    }
}
