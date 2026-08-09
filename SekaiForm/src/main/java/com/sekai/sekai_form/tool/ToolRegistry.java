package com.sekai.sekai_form.tool;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.util.LinkedHashMap;
import java.util.Map;

public class ToolRegistry {
    private final Map<String, Tool> tools = new LinkedHashMap<>();
    
    public ToolRegistry register(Tool tool) {
        tools.put(tool.name(), tool);
        return this;
    }
    
    public ArrayNode getToolDefinitions() {
        ArrayNode array = JsonNodeFactory.instance.arrayNode();
        for (Tool tool : tools.values()) {
            array.add(tool.getDefinition());
        }
        return array;
    }
    
    public Tool get(String functionName) {
        return tools.get(functionName);
    }
    
    public boolean isEmpty() {
        return tools.isEmpty();
    }
    
    public int size() {
        return tools.size();
    }
}
