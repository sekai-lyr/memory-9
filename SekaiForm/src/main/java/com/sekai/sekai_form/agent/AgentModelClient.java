package com.sekai.sekai_form.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.sekai.sekai_form.dataobject.Live2DChatConfigDO;

public interface AgentModelClient {
    JsonNode complete(Live2DChatConfigDO config, ArrayNode messages, ArrayNode tools);
}
