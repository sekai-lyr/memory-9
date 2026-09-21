package com.sekai.sekai_form.dataobject;

import java.time.LocalDateTime;

public class Live2DChatConfigDO {
    private Long id;
    private Long modelId;
    private String apiUrl;
    private String apiKey;
    private String modelName;
    private String systemPrompt;
    private Integer maxTokens;
    private Double temperature;
    private String imageModelName;
    private String imageApiUrl;
    private Boolean enabled;
    private LocalDateTime createTime;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getModelId() { return modelId; }
    public void setModelId(Long modelId) { this.modelId = modelId; }
    public String getApiUrl() { return apiUrl; }
    public void setApiUrl(String apiUrl) { this.apiUrl = apiUrl; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public String getSystemPrompt() { return systemPrompt; }
    public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }
    public Integer getMaxTokens() { return maxTokens; }
    public void setMaxTokens(Integer maxTokens) { this.maxTokens = maxTokens; }
    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }
    public String getImageModelName() { return imageModelName; }
    public void setImageModelName(String imageModelName) { this.imageModelName = imageModelName; }
    public String getImageApiUrl() { return imageApiUrl; }
    public void setImageApiUrl(String imageApiUrl) { this.imageApiUrl = imageApiUrl; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
