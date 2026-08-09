package com.sekai.sekai_form.tool;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class NewsTool implements Tool {
    private static final String API_URL = "https://uapis.cn/api/v1/misc/hotboard";
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final HttpClient httpClient = HttpClient.newHttpClient();

    @Override public String name() { return "get_news"; }
    @Override public String description() {
        return "\u67e5\u8be2\u70ed\u641c\u65b0\u95fb\uff0c\u652f\u6301weibo/zhihu/baidu/douyin/bilibili/toutiao";
    }
    @Override public ObjectNode getParametersSchema() {
        ObjectNode params = JsonNodeFactory.instance.objectNode();
        params.put("type", "object");
        ObjectNode props = JsonNodeFactory.instance.objectNode();
        ObjectNode type = JsonNodeFactory.instance.objectNode();
        type.put("type", "string");
        type.put("description", "\u5e73\u53f0\uff1aweibo/zhihu/baidu/douyin/bilibili/toutiao\uff0c\u9ed8\u8ba4baidu");
        props.set("type", type);
        ObjectNode count = JsonNodeFactory.instance.objectNode();
        count.put("type", "integer");
        count.put("description", "\u8fd4\u56de\u6761\u6570\uff0c\u9ed8\u8ba45\u6761");
        props.set("count", count);
        params.set("properties", props);
        return params;
    }
    @Override public String execute(ObjectNode args) {
        String type = args.has("type") ? args.get("type").asText().trim().toLowerCase() : "baidu";
        int count = args.has("count") ? Math.min(args.get("count").asInt(), 10) : 5;
        if (count <= 0) count = 5;
        String label = switch (type) {
            case "weibo" -> "\u5fae\u535a"; case "zhihu" -> "\u77e5\u4e4e";
            case "baidu" -> "\u767e\u5ea6"; case "douyin" -> "\u6296\u97f3";
            case "bilibili" -> "B\u7ad9"; case "toutiao" -> "\u5934\u6761";
            default -> "\u767e\u5ea6";
        };
        try {
            String url = API_URL + "?type=" + type + "&limit=" + count;
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            JsonNode json = mapper.readTree(resp.body());
            JsonNode list = json.has("list") ? json.get("list") : json.has("data") && json.get("data").has("list") ? json.get("data").get("list") : null;
            if (list == null || list.isEmpty()) return label + "\u70ed\u641c\u6682\u65e0\u6570\u636e";
            StringBuilder sb = new StringBuilder(label + "\u70ed\u641c\u699c\uff1a\n\n");
            int max = Math.min(list.size(), count);
            for (int i = 0; i < max; i++) {
                JsonNode item = list.get(i);
                String title = item.has("title") ? item.get("title").asText() : "";
                String hot = item.has("hot_value") ? item.get("hot_value").asText() : "";
                sb.append(i + 1).append(". ").append(title);
                if (!hot.isBlank()) sb.append(" \ud83d\udd25").append(formatHot(hot));
                sb.append("\n");
            }
            return sb.toString().trim();
        } catch (Exception e) {
            return "\u70ed\u641c\u67e5\u8be2\u51fa\u9519: " + e.getMessage();
        }
    }
    private static String formatHot(String v) {
        try {
            long n = Long.parseLong(v);
            if (n >= 10000) return String.format("%.1f\u4e07", n / 10000.0);
            if (n >= 1000) return String.format("%.1fk", n / 1000.0);
            return String.valueOf(n);
        } catch (NumberFormatException e) { return v; }
    }
}
