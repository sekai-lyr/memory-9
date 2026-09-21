package com.sekai.sekai_form.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sekai.sekai_form.dataobject.Live2DChatConfigDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;

import java.time.Duration;

@Service
public class OpenAiCompatibleAgentModelClient implements AgentModelClient {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(OpenAiCompatibleAgentModelClient.class);
    private final ObjectMapper mapper;
    private final RestTemplate restTemplate;

    @Autowired
    public OpenAiCompatibleAgentModelClient(ObjectMapper mapper) {
        this(mapper, createRestTemplate());
    }

    OpenAiCompatibleAgentModelClient(ObjectMapper mapper, RestTemplate restTemplate) {
        this.mapper = mapper;
        this.restTemplate = restTemplate;
    }

    private static RestTemplate createRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(45));
        return new RestTemplate(factory);
    }

    @Override
    public JsonNode complete(Live2DChatConfigDO config, ArrayNode messages, ArrayNode tools) {
        if (config == null || blank(config.getApiUrl()) || blank(config.getApiKey())) {
            throw new IllegalStateException("AI 对话未配置");
        }
        ObjectNode body = mapper.createObjectNode();
        body.put("model", blank(config.getModelName()) ? "deepseek-chat" : config.getModelName());
        body.put("max_tokens", config.getMaxTokens() == null ? 1024 : config.getMaxTokens());
        body.put("temperature", config.getTemperature() == null ? 0.7 : config.getTemperature());
        body.set("messages", messages);
        if (tools != null && !tools.isEmpty()) {
            body.set("tools", tools);
            body.put("tool_choice", "auto");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(normalizeApiKey(config.getApiKey()));
        HttpEntity<String> entity;
        try {
            entity = new HttpEntity<>(mapper.writeValueAsString(body), headers);
            String endpoint = normalizeEndpoint(config.getApiUrl());
            ResponseEntity<String> response = restTemplate.exchange(endpoint, HttpMethod.POST, entity, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new IllegalStateException("模型服务暂时不可用");
            }
            JsonNode parsed = mapper.readTree(response.getBody());
            if (!parsed.path("choices").isArray() && !parsed.path("output").path("choices").isArray()) {
                throw new IllegalStateException("模型返回格式无效");
            }
            return parsed;
        } catch (HttpStatusCodeException ex) {
            int status = ex.getStatusCode().value();
            logger.warn("Model endpoint returned HTTP status={}", status);
            throw new IllegalStateException(messageForStatus(status));
        } catch (ResourceAccessException ex) {
            logger.warn("Model endpoint is unreachable: {}", ex.getClass().getSimpleName());
            throw new IllegalStateException("无法连接模型服务，请检查网络或 API URL。", ex);
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("模型服务请求失败", ex);
        }
    }

    private static String normalizeEndpoint(String value) {
        String endpoint = value == null ? "" : value.trim();
        if (!(endpoint.startsWith("http://") || endpoint.startsWith("https://"))) {
            throw new IllegalStateException("API URL 必须是 http 或 https 地址。");
        }
        while (endpoint.endsWith("/")) endpoint = endpoint.substring(0, endpoint.length() - 1);
        return endpoint.endsWith("/chat/completions") ? endpoint : endpoint + "/chat/completions";
    }

    private static String normalizeApiKey(String value) {
        String key = value == null ? "" : value.trim();
        return key.regionMatches(true, 0, "Bearer ", 0, 7) ? key.substring(7).trim() : key;
    }

    private static String messageForStatus(int status) {
        if (status == 400) return "模型请求参数无效，请检查模型配置。";
        if (status == 401 || status == 403) return "模型鉴权失败，请检查 API Key。";
        if (status == 404) return "模型接口地址无效，请检查 API URL。";
        if (status == 429) return "模型服务频率受限，请稍后重试。";
        if (status >= 500) return "模型服务暂时不可用，请稍后重试。";
        return "模型服务请求失败。";
    }

    private static boolean blank(String value) { return value == null || value.isBlank(); }
}
