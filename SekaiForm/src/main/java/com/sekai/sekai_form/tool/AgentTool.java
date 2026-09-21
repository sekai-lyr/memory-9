package com.sekai.sekai_form.tool;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sekai.sekai_form.agent.AgentExecutionContext;
import com.sekai.sekai_form.agent.ToolResult;

/** Context-aware tool contract used by the Agent loop. */
public interface AgentTool {
    String name();
    String description();
    ObjectNode getParametersSchema();
    ToolResult<?> execute(ObjectNode arguments, AgentExecutionContext context);

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
