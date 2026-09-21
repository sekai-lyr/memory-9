package com.sekai.sekai_form.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;

public final class ToolCall {
    private final String id;
    private final String name;
    private final ObjectNode arguments;

    public ToolCall(String id, String name, ObjectNode arguments) {
        this.id = id == null || id.isBlank() ? "call" : id;
        this.name = name == null ? "" : name;
        this.arguments = arguments == null ? com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode() : arguments;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getToolName() { return name; }
    public ObjectNode getArguments() { return arguments; }

    public static List<ToolCall> parse(JsonNode response, ObjectMapper mapper) {
        List<ToolCall> result = new ArrayList<>();
        JsonNode message = response == null ? null : response.path("choices").path(0).path("message");
        if (!message.isObject()) message = response == null ? null : response.path("output").path("choices").path(0).path("message");
        if (message == null || !message.isObject() || !message.path("tool_calls").isArray()) return result;
        for (JsonNode call : message.path("tool_calls")) {
            JsonNode function = call.path("function");
            String name = function.path("name").asText("");
            JsonNode rawArguments = function.path("arguments");
            ObjectNode args;
            if (rawArguments.isObject()) {
                args = (ObjectNode) rawArguments.deepCopy();
            } else {
                String raw = rawArguments.asText("{}");
                try {
                    JsonNode parsed = raw.isBlank() ? mapper.createObjectNode() : mapper.readTree(raw);
                    args = parsed != null && parsed.isObject() ? (ObjectNode) parsed : mapper.createObjectNode();
                } catch (Exception ignored) {
                    args = mapper.createObjectNode();
                    args.put("_invalid_arguments", true);
                }
            }
            result.add(new ToolCall(call.path("id").asText("call"), name, args));
        }
        return result;
    }

    public static JsonNode assistantMessage(JsonNode response) {
        JsonNode message = response == null ? null : response.path("choices").path(0).path("message");
        if (!message.isObject()) message = response == null ? null : response.path("output").path("choices").path(0).path("message");
        return message == null || !message.isObject() ? null : message;
    }
}
