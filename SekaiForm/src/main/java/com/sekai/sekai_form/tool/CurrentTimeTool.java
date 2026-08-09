package com.sekai.sekai_form.tool;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class CurrentTimeTool implements Tool {
    @Override public String name() { return "get_current_time"; }
    @Override public String description() {
        return "查询当前日期和时间，可指定时区如Asia/Shanghai、America/New_York";
    }
    @Override public ObjectNode getParametersSchema() {
        ObjectNode params = JsonNodeFactory.instance.objectNode();
        params.put("type", "object");
        ObjectNode props = JsonNodeFactory.instance.objectNode();
        ObjectNode tz = JsonNodeFactory.instance.objectNode();
        tz.put("type", "string");
        tz.put("description", "时区ID，默认Asia/Shanghai");
        props.set("timezone", tz);
        params.set("properties", props);
        return params;
    }
    @Override public String execute(ObjectNode args) {
        String tzStr = args.has("timezone") ? args.get("timezone").asText() : "Asia/Shanghai";
        try {
            ZonedDateTime now = ZonedDateTime.now(ZoneId.of(tzStr));
            return now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z (EEEE)"));
        } catch (Exception e) {
            return "无效时区: " + tzStr;
        }
    }
}
