package com.sekai.sekai_form.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sekai.sekai_form.agent.AgentAttachment;
import com.sekai.sekai_form.dataobject.Live2DChatConfigDO;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Base64;

@Service
public class ImageGenerationService implements ImageGenerationGateway {
    private final ObjectMapper mapper;
    private final RestTemplate restTemplate;

    public ImageGenerationService(ObjectMapper mapper) {
        this.mapper = mapper;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(30));
        this.restTemplate = new RestTemplate(factory);
    }

    @Override public String generate(Live2DChatConfigDO config, String prompt) { return submit(config, prompt, null); }
    @Override public String edit(Live2DChatConfigDO config, String prompt, AgentAttachment source) { return submit(config, prompt, source); }

    private String submit(Live2DChatConfigDO config, String prompt, AgentAttachment source) {
        if (config == null || config.getApiKey() == null || config.getApiKey().isBlank()) throw new IllegalStateException("图片服务未配置");
        String base = config.getImageApiUrl();
        if (base == null || base.isBlank()) base = config.getApiUrl();
        if (base == null || base.isBlank()) throw new IllegalStateException("图片服务未配置");
        base = base.replace("/compatible-mode/v1", "").replace("/compatible-mode", "")
                .replace("/chat/completions", "").replaceAll("/$", "");
        ObjectNode input = mapper.createObjectNode();
        input.put("prompt", prompt == null || prompt.isBlank() ? "Live2D 角色插画" : prompt);
        if (source != null) {
            ArrayNode refs = mapper.createArrayNode();
            refs.add("data:" + source.getMediaType() + ";base64," + Base64.getEncoder().encodeToString(source.getContent()));
            input.set("ref_images", refs);
        }
        ObjectNode parameters = mapper.createObjectNode();
        parameters.put("size", "1024*1024");
        parameters.put("n", 1);
        ObjectNode body = mapper.createObjectNode();
        body.put("model", config.getImageModelName() == null || config.getImageModelName().isBlank() ? "wanx2.1-t2i-turbo" : config.getImageModelName());
        body.set("input", input);
        body.set("parameters", parameters);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(config.getApiKey());
        headers.set("X-DashScope-Async", "enable");
        try {
            ResponseEntity<String> created = restTemplate.exchange(base + "/api/v1/services/aigc/text2image/image-synthesis", HttpMethod.POST,
                    new HttpEntity<>(mapper.writeValueAsString(body), headers), String.class);
            String taskId = mapper.readTree(created.getBody()).path("output").path("task_id").asText("");
            if (taskId.isBlank()) throw new IllegalStateException("图片任务未创建");
            for (int i = 0; i < 60; i++) {
                Thread.sleep(1000);
                ResponseEntity<String> statusResponse = restTemplate.exchange(base + "/api/v1/tasks/" + taskId, HttpMethod.GET, new HttpEntity<>(headers), String.class);
                JsonNode output = mapper.readTree(statusResponse.getBody()).path("output");
                String status = output.path("task_status").asText("");
                if ("SUCCEEDED".equalsIgnoreCase(status)) {
                    String url = output.path("results").path(0).path("url").asText("");
                    if (!url.isBlank()) return url;
                    throw new IllegalStateException("图片结果为空");
                }
                if ("FAILED".equalsIgnoreCase(status)) throw new IllegalStateException("图片任务失败");
            }
            throw new IllegalStateException("图片任务超时");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("图片任务已停止", ex);
        } catch (IllegalStateException ex) { throw ex;
        } catch (Exception ex) { throw new IllegalStateException("图片服务请求失败", ex); }
    }
}
