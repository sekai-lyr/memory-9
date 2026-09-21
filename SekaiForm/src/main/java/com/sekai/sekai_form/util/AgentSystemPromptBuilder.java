package com.sekai.sekai_form.util;

import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Loads the shared Agent prompt and replaces only the declared runtime variables.
 */
public final class AgentSystemPromptBuilder {
    private static final String TEMPLATE_PATH = "prompts/general-multimodal-agent.txt";
    private final String template;

    public AgentSystemPromptBuilder() {
        try (InputStream input = new ClassPathResource(TEMPLATE_PATH).getInputStream()) {
            template = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("无法加载 Agent 系统提示词模板", e);
        }
    }

    public String build(Map<String, String> variables) {
        String prompt = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String value = entry.getValue() == null ? "" : entry.getValue();
            prompt = prompt.replace("{" + entry.getKey() + "}", value);
        }
        return prompt;
    }
}
