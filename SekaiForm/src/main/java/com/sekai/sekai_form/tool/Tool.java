package com.sekai.sekai_form.tool;

import com.fasterxml.jackson.databind.node.ObjectNode;

public interface Tool {
    String name();
    String description();
    ObjectNode getParametersSchema();
    String execute(ObjectNode arguments);
    
    default ObjectNode getDefinition() {
        ObjectNode function = com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode();
        function.put("name", name());
        function.put("description", description());
        function.set("parameters", getParametersSchema());
        
        ObjectNode tool = com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode();
        tool.put("type", "function");
        tool.set("function", function);
        return tool;
    }
}
