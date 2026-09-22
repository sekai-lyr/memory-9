package com.sekai.sekai_form.config;

import com.sekai.sekai_form.dataobject.Live2DChatConfigDO;
import com.sekai.sekai_form.dataobject.SekaiFormFoodDO;
import com.sekai.sekai_form.mapper.Live2DChatConfigMapper;
import com.sekai.sekai_form.mapper.SekaiFormFoodMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {
    private final SekaiFormFoodMapper foodMapper;
    private final Live2DChatConfigMapper chatConfigMapper;

    @Value("${AI_API_URL:}")
    private String aiApiUrlProperty;
    @Value("${AI_API_KEY:}")
    private String aiApiKeyProperty;
    @Value("${DEEPSEEK_API_KEY:}")
    private String deepSeekApiKeyProperty;
    @Value("${DASHSCOPE_API_KEY:}")
    private String dashScopeApiKeyProperty;
    @Value("${AI_MODEL:}")
    private String aiModelProperty;
    @Value("${agent.model.api-url:}")
    private String agentApiUrlProperty;
    @Value("${agent.model.api-key:}")
    private String agentApiKeyProperty;
    @Value("${agent.model.name:}")
    private String agentModelProperty;
    @Value("${anime.llm.base-url:}")
    private String animeLlmBaseUrlProperty;
    @Value("${anime.llm.api-key:}")
    private String animeLlmApiKeyProperty;
    @Value("${anime.llm.model:}")
    private String animeLlmModelProperty;
    @Value("${ilink.llm.base-url:}")
    private String ilinkLlmBaseUrlProperty;
    @Value("${ilink.llm.api-key:}")
    private String ilinkLlmApiKeyProperty;
    @Value("${ilink.llm.model:}")
    private String ilinkLlmModelProperty;
    @Value("${spring.ai.openai.base-url:}")
    private String springAiBaseUrlProperty;
    @Value("${spring.ai.openai.api-key:}")
    private String springAiApiKeyProperty;
    @Value("${spring.ai.openai.chat.options.model:}")
    private String springAiModelProperty;

    public DataInitializer(SekaiFormFoodMapper foodMapper, Live2DChatConfigMapper chatConfigMapper) {
        this.foodMapper = foodMapper;
        this.chatConfigMapper = chatConfigMapper;
    }
    @Override
    public void run(String... args) {
        if (foodMapper.listAll().isEmpty()) {
            SekaiFormFoodDO f1 = new SekaiFormFoodDO(); f1.setName("苹果"); f1.setSatietyValue(10); f1.setMoodValue(5); foodMapper.insert(f1);
            SekaiFormFoodDO f2 = new SekaiFormFoodDO(); f2.setName("蛋糕"); f2.setSatietyValue(20); f2.setMoodValue(15); f2.setAffectionValue(10); foodMapper.insert(f2);
            SekaiFormFoodDO f3 = new SekaiFormFoodDO(); f3.setName("糖果"); f3.setSatietyValue(8); f3.setMoodValue(10); foodMapper.insert(f3);
        }
        // Initialize the default OpenAI-compatible chat config for modelId=1
        Live2DChatConfigDO existing = chatConfigMapper.getByModelId(1L);
        String explicitUrl = firstNonBlank(env("AI_API_URL"), aiApiUrlProperty,
                env("ANIME_LLM_BASE_URL"), animeLlmBaseUrlProperty,
                env("ILINK_LLM_BASE_URL"), ilinkLlmBaseUrlProperty,
                springAiBaseUrlProperty);
        String aiKey = firstNonBlank(env("AI_API_KEY"), aiApiKeyProperty,
                env("DEEPSEEK_API_KEY"), deepSeekApiKeyProperty,
                env("ANIME_LLM_API_KEY"), animeLlmApiKeyProperty,
                env("ILINK_LLM_API_KEY"), ilinkLlmApiKeyProperty,
                springAiApiKeyProperty);
        String dashScopeKey = firstNonBlank(env("DASHSCOPE_API_KEY"), dashScopeApiKeyProperty);
        String model = firstNonBlank(env("AI_MODEL"), aiModelProperty,
                agentModelProperty, animeLlmModelProperty, ilinkLlmModelProperty,
                springAiModelProperty);
        boolean useDashScope = aiKey.isBlank() && !dashScopeKey.isBlank()
                && (explicitUrl.isBlank() || explicitUrl.toLowerCase().contains("dashscope"));
        String resolvedUrl = explicitUrl.isBlank()
                ? (useDashScope ? "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"
                : "https://api.deepseek.com/v1/chat/completions")
                : explicitUrl;
        String resolvedKey = useDashScope ? dashScopeKey : aiKey;
        String resolvedModel = firstNonBlank(model, useDashScope ? "qwen-plus" : "deepseek-chat");
        if (existing == null) {
            Live2DChatConfigDO config = new Live2DChatConfigDO();
            config.setModelId(1L);
            config.setApiUrl(resolvedUrl);
            config.setApiKey(resolvedKey);
            config.setModelName(resolvedModel);
            config.setMaxTokens(1024);
            config.setTemperature(0.85);
            config.setEnabled(true);
            chatConfigMapper.insert(config);
        } else if (!aiKey.isBlank() && isAutoSeededDashScope(existing, dashScopeKey)) {
            // Switch the auto-seeded DashScope record when an explicit DeepSeek key
            // is supplied. User-saved records are never overwritten by this path.
            existing.setApiUrl(resolvedUrl);
            existing.setApiKey(resolvedKey);
            existing.setModelName(resolvedModel);
            chatConfigMapper.update(existing);
        } else if (!aiKey.isBlank() && isAutoSeededDeepSeek(existing, aiKey)) {
            existing.setApiUrl(resolvedUrl);
            existing.setApiKey(resolvedKey);
            existing.setModelName(resolvedModel);
            chatConfigMapper.update(existing);
        } else if (isLegacyDashScopeSeed(existing, dashScopeKey)) {
            // Previous versions paired DASHSCOPE_API_KEY with the DeepSeek endpoint.
            // Migrate only that exact auto-seeded shape; never overwrite a user-saved key.
            existing.setApiUrl("https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions");
            existing.setApiKey(dashScopeKey);
            existing.setModelName(firstNonBlank(model, "qwen-plus"));
            chatConfigMapper.update(existing);
        } else {
            boolean changed = false;
            if (blank(existing.getApiUrl()) && !explicitUrl.isBlank()) {
                existing.setApiUrl(explicitUrl);
                changed = true;
            }
            if (blank(existing.getApiKey()) && !resolvedKey.isBlank()) {
                existing.setApiKey(resolvedKey);
                changed = true;
            }
            if (blank(existing.getModelName()) && !model.isBlank()) {
                existing.setModelName(model);
                changed = true;
            }
            if (changed) chatConfigMapper.update(existing);
        }
    }

    private static String env(String name) {
        String value = System.getenv(name);
        return value == null ? "" : value.trim();
    }

    private static String firstNonBlank(String... values) {
        if (values != null) for (String value : values) if (value != null && !value.isBlank()) return value.trim();
        return "";
    }

    private static boolean isLegacyDashScopeSeed(Live2DChatConfigDO config, String dashScopeKey) {
        return config != null && !dashScopeKey.isBlank()
                && dashScopeKey.equals(config.getApiKey())
                && config.getApiUrl() != null
                && config.getApiUrl().contains("api.deepseek.com");
    }

    private static boolean isAutoSeededDashScope(Live2DChatConfigDO config, String dashScopeKey) {
        return config != null && !dashScopeKey.isBlank()
                && dashScopeKey.equals(config.getApiKey())
                && config.getApiUrl() != null
                && config.getApiUrl().contains("dashscope.aliyuncs.com");
    }

    private static boolean isAutoSeededDeepSeek(Live2DChatConfigDO config, String deepSeekKey) {
        return config != null && !deepSeekKey.isBlank()
                && deepSeekKey.equals(config.getApiKey())
                && config.getApiUrl() != null
                && config.getApiUrl().contains("api.deepseek.com");
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
