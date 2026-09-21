import pathlib

# Create CurrentTimeTool.java
tool = '''package com.sekai.sekai_form.tool;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class CurrentTimeTool implements Tool {
    @Override public String name() { return "get_current_time"; }
    @Override public String description() {
        return "\u67e5\u8be2\u5f53\u524d\u65e5\u671f\u548c\u65f6\u95f4\uff0c\u53ef\u6307\u5b9a\u65f6\u533a\u5982Asia/Shanghai\u3001America/New_York";
    }
    @Override public ObjectNode getParametersSchema() {
        ObjectNode params = JsonNodeFactory.instance.objectNode();
        params.put("type", "object");
        ObjectNode props = JsonNodeFactory.instance.objectNode();
        ObjectNode tz = JsonNodeFactory.instance.objectNode();
        tz.put("type", "string");
        tz.put("description", "\u65f6\u533aID\uff0c\u9ed8\u8ba4Asia/Shanghai");
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
            return "\u65e0\u6548\u65f6\u533a: " + tzStr;
        }
    }
}
'''
pathlib.Path(r'D:\Sekai_two\memory-9\SekaiForm\src\main\java\com\sekai\sekai_form\tool\CurrentTimeTool.java').write_text(tool, 'utf-8')
print('Created CurrentTimeTool')
