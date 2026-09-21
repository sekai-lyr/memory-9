package com.sekai.sekai_form.agent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class AgentRequest {
    private final Long modelId;
    private final String conversationId;
    private final String userId;
    private final String message;
    private final String modality;
    private final List<AgentAttachment> attachments;
    private final List<Map<String, Object>> externalHistory;
    private final String sessionState;
    private final List<String> forcedToolNames;

    private AgentRequest(Builder builder) {
        this.modelId = builder.modelId;
        this.conversationId = builder.conversationId;
        this.userId = builder.userId;
        this.message = builder.message == null ? "" : builder.message;
        this.modality = builder.modality == null ? "TEXT" : builder.modality;
        this.attachments = Collections.unmodifiableList(new ArrayList<>(builder.attachments));
        this.externalHistory = Collections.unmodifiableList(new ArrayList<>(builder.externalHistory));
        this.sessionState = builder.sessionState == null ? "active" : builder.sessionState;
        this.forcedToolNames = Collections.unmodifiableList(new ArrayList<>(builder.forcedToolNames));
    }

    public static Builder builder() { return new Builder(); }
    public Long getModelId() { return modelId; }
    public String getConversationId() { return conversationId; }
    public String getUserId() { return userId; }
    public String getMessage() { return message; }
    public String getModality() { return modality; }
    public List<AgentAttachment> getAttachments() { return attachments; }
    public List<Map<String, Object>> getExternalHistory() { return externalHistory; }
    public String getSessionState() { return sessionState; }
    public List<String> getForcedToolNames() { return forcedToolNames; }

    public static final class Builder {
        private Long modelId;
        private String conversationId = "anonymous";
        private String userId = "anonymous";
        private String message;
        private String modality = "TEXT";
        private List<AgentAttachment> attachments = new ArrayList<>();
        private List<Map<String, Object>> externalHistory = new ArrayList<>();
        private String sessionState = "active";
        private List<String> forcedToolNames = new ArrayList<>();

        public Builder modelId(Long value) { modelId = value; return this; }
        public Builder conversationId(String value) { conversationId = value; return this; }
        public Builder userId(String value) { userId = value; return this; }
        public Builder message(String value) { message = value; return this; }
        public Builder modality(String value) { modality = value; return this; }
        public Builder attachments(List<AgentAttachment> value) { attachments = value == null ? new ArrayList<>() : value; return this; }
        public Builder addAttachment(AgentAttachment value) { if (value != null) attachments.add(value); return this; }
        public Builder externalHistory(List<Map<String, Object>> value) { externalHistory = value == null ? new ArrayList<>() : value; return this; }
        public Builder sessionState(String value) { sessionState = value; return this; }
        public Builder forcedToolNames(List<String> value) { forcedToolNames = value == null ? new ArrayList<>() : value; return this; }
        public AgentRequest build() { return new AgentRequest(this); }
    }
}
