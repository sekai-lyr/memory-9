package com.sekai.sekai_form.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sekai.sekai_form.dataobject.Live2DChatConfigDO;
import com.sekai.sekai_form.service.AgentSessionStateService;

public final class AgentExecutionContext {
    private final AgentRequest request;
    private final Live2DChatConfigDO config;
    private final ObjectMapper objectMapper;
    private final AgentSessionStateService sessionStateService;

    public AgentExecutionContext(AgentRequest request, Live2DChatConfigDO config, ObjectMapper objectMapper,
                                 AgentSessionStateService sessionStateService) {
        this.request = request;
        this.config = config;
        this.objectMapper = objectMapper;
        this.sessionStateService = sessionStateService;
    }

    public AgentRequest getRequest() { return request; }
    public Live2DChatConfigDO getConfig() { return config; }
    public ObjectMapper getObjectMapper() { return objectMapper; }
    public AgentSessionStateService getSessionStateService() { return sessionStateService; }
}
