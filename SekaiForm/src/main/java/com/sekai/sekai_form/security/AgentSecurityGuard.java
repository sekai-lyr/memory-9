package com.sekai.sekai_form.security;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

@Component
public class AgentSecurityGuard {
    private static final Pattern WINDOWS_PATH = Pattern.compile("(?i)[a-z]:\\\\[^\\s]+|[a-z]:/[^\\s]+");
    private static final Pattern UNIX_PATH = Pattern.compile("(?<!https?:)(?<![\\w])/(?:[^\\s/]+/)+[^\\s]*");
    private static final Pattern REMOTE_URL = Pattern.compile("https?://[^\\s]+", Pattern.CASE_INSENSITIVE);
    private static final Pattern SECRET = Pattern.compile("(?i)(bearer\\s+|api[_-]?key\\s*[:=]|sk-[a-z0-9_-]{8,}|password\\s*[:=])[^\\s,，。;；]+", Pattern.CASE_INSENSITIVE);
    private static final Pattern INTERNAL_ID = Pattern.compile("(?i)(conversation[_-]?id|trace[_-]?id|tool[_-]?call[_-]?id)\\s*[:=]\\s*[\\w-]+");

    public String clean(String value) {
        if (value == null) return "";
        java.util.List<String> urls = new java.util.ArrayList<>();
        java.util.regex.Matcher matcher = REMOTE_URL.matcher(value);
        StringBuffer protectedValue = new StringBuffer();
        while (matcher.find()) {
            urls.add(matcher.group());
            matcher.appendReplacement(protectedValue, "__REMOTE_URL_" + (urls.size() - 1) + "__");
        }
        matcher.appendTail(protectedValue);
        String result = SECRET.matcher(protectedValue.toString()).replaceAll("[已隐藏]");
        result = WINDOWS_PATH.matcher(result).replaceAll("[内部文件]");
        result = UNIX_PATH.matcher(result).replaceAll("[内部资源]");
        result = INTERNAL_ID.matcher(result).replaceAll("$1: [内部状态]");
        for (int i = 0; i < urls.size(); i++) result = result.replace("__REMOTE_URL_" + i + "__", urls.get(i));
        return result;
    }

    public ObjectNode cleanArguments(ObjectNode source) {
        ObjectNode copy = source == null ? com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode() : source.deepCopy();
        cleanNode(copy);
        return copy;
    }

    private void cleanNode(ObjectNode node) {
        Iterator<Map.Entry<String, com.fasterxml.jackson.databind.JsonNode>> iterator = node.fields();
        while (iterator.hasNext()) {
            Map.Entry<String, com.fasterxml.jackson.databind.JsonNode> entry = iterator.next();
            if (entry.getValue().isTextual()) node.put(entry.getKey(), clean(entry.getValue().asText()));
            else if (entry.getValue().isObject()) cleanNode((ObjectNode) entry.getValue());
        }
    }
}
