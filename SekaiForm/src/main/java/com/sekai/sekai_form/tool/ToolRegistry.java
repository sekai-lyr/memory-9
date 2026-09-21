package com.sekai.sekai_form.tool;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.sekai.sekai_form.agent.AgentExecutionContext;
import com.sekai.sekai_form.agent.ToolResult;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.LinkedHashMap;

@Component
public class ToolRegistry {
    private static final Map<String, String> COMPATIBLE_NAMES = Map.ofEntries(
            Map.entry("getCurrentTime", "get_current_time"),
            Map.entry("getWeather", "get_weather"),
            Map.entry("tellJoke", "tell_joke"),
            Map.entry("getNews", "get_news"),
            Map.entry("getConstellation", "get_constellation"),
            Map.entry("analyzeImage", "analyze_image"),
            Map.entry("generateImage", "generate_image"),
            Map.entry("editImage", "edit_image"),
            Map.entry("analyzeFile", "analyze_file"),
            Map.entry("transcribeAudio", "transcribe_audio"),
            Map.entry("synthesizeSpeech", "synthesize_speech")
    );
    private final Map<String, Tool> tools = new LinkedHashMap<>();
    private final Map<String, AgentTool> agentTools = new LinkedHashMap<>();
    
    public ToolRegistry register(Tool tool) {
        if (tool != null) tools.put(tool.name(), tool);
        return this;
    }

    public ToolRegistry register(AgentTool tool) {
        if (tool != null) agentTools.put(tool.name(), tool);
        return this;
    }
    
    public ArrayNode getToolDefinitions() {
        ArrayNode array = JsonNodeFactory.instance.arrayNode();
        for (Tool tool : tools.values()) {
            array.add(tool.getDefinition());
        }
        for (AgentTool tool : agentTools.values()) {
            array.add(tool.getDefinition());
        }
        return array;
    }
    
    public Tool get(String functionName) {
        return tools.get(canonicalName(functionName));
    }

    public AgentTool getAgentTool(String functionName) {
        return agentTools.get(canonicalName(functionName));
    }

    public JsonNode getDefinition(String functionName) {
        String canonical = canonicalName(functionName);
        Tool legacy = tools.get(canonical);
        if (legacy != null) return legacy.getDefinition();
        AgentTool contextTool = agentTools.get(canonical);
        return contextTool == null ? null : contextTool.getDefinition();
    }

    public String validateArguments(String functionName, com.fasterxml.jackson.databind.node.ObjectNode arguments) {
        JsonNode definition = getDefinition(functionName);
        if (definition == null) return "工具不可用";
        JsonNode schema = definition.path("function").path("parameters");
        for (JsonNode required : schema.path("required")) {
            if (!arguments.has(required.asText()) || arguments.get(required.asText()).isNull()) return "缺少必要参数";
        }
        JsonNode properties = schema.path("properties");
        java.util.Iterator<String> fields = arguments.fieldNames();
        while (fields.hasNext()) {
            String field = fields.next();
            JsonNode expected = properties.path(field);
            if (expected.isMissingNode()) return "包含未声明参数";
            String type = expected.path("type").asText();
            JsonNode value = arguments.get(field);
            boolean valid = switch (type) {
                case "string" -> value.isTextual();
                case "integer" -> value.isIntegralNumber();
                case "number" -> value.isNumber();
                case "boolean" -> value.isBoolean();
                case "array" -> value.isArray();
                case "object" -> value.isObject();
                default -> true;
            };
            if (!valid) return "参数类型不正确";
        }
        return null;
    }

    public ToolResult<?> execute(String functionName, com.fasterxml.jackson.databind.node.ObjectNode arguments,
                                 AgentExecutionContext context) {
        String canonical = canonicalName(functionName);
        AgentTool contextTool = agentTools.get(canonical);
        if (contextTool != null) return contextTool.execute(arguments, context);
        Tool legacyTool = tools.get(canonical);
        if (legacyTool == null) return ToolResult.failure("工具不可用", false);
        try {
            String value = legacyTool.execute(arguments);
            if (value == null) return ToolResult.failure("工具未返回结果", true);
            String lower = value.toLowerCase(java.util.Locale.ROOT);
            boolean failed = lower.contains("失败") || lower.contains("出错") || lower.contains("不可用") || lower.contains("error");
            return failed ? ToolResult.failure(value, true) : ToolResult.success(value);
        } catch (Exception ex) {
            return ToolResult.failure("工具执行失败", true);
        }
    }
    
    public boolean isEmpty() {
        return tools.isEmpty() && agentTools.isEmpty();
    }
    
    public int size() {
        return tools.size() + agentTools.size();
    }

    /** Resolves the camelCase names used by memory-14 and the prompt to local names. */
    public String canonicalName(String functionName) {
        if (functionName == null) return "";
        String value = functionName.trim();
        return COMPATIBLE_NAMES.getOrDefault(value, value);
    }
}
