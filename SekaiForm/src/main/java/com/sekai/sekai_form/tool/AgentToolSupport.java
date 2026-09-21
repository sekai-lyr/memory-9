package com.sekai.sekai_form.tool;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

final class AgentToolSupport {
    private AgentToolSupport() { }
    static ObjectNode schema(String[][] fields, String... required) {
        ObjectNode schema = JsonNodeFactory.instance.objectNode();
        schema.put("type", "object");
        ObjectNode properties = JsonNodeFactory.instance.objectNode();
        for (String[] field : fields) {
            ObjectNode item = JsonNodeFactory.instance.objectNode();
            item.put("type", field.length > 1 ? field[1] : "string");
            if (field.length > 2) item.put("description", field[2]);
            properties.set(field[0], item);
        }
        schema.set("properties", properties);
        ArrayNode requiredNode = JsonNodeFactory.instance.arrayNode();
        for (String item : required) requiredNode.add(item);
        schema.set("required", requiredNode);
        return schema;
    }
}
