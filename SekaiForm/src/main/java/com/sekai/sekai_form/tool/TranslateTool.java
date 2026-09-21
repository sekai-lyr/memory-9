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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class TranslateTool implements Tool {
    private static final String API_URL = "https://uapis.cn/api/v1/ai/translate";
    private static final ObjectMapper mapper = new ObjectMapper();
    private static HttpClient httpClient() { return HttpClient.newHttpClient(); }

    @Override public String name() { return "translate"; }
    @Override public String description() {
        return "\u7ffb\u8bd1\u6587\u672c\u5230\u6307\u5b9a\u8bed\u8a00\uff0c\u652f\u6301zh/en/ja/ko/fr/de/es\u7b49";
    }
    @Override public ObjectNode getParametersSchema() {
        ObjectNode params = JsonNodeFactory.instance.objectNode();
        params.put("type", "object");
        ObjectNode props = JsonNodeFactory.instance.objectNode();
        ObjectNode text = JsonNodeFactory.instance.objectNode();
        text.put("type", "string");
        text.put("description", "\u8981\u7ffb\u8bd1\u7684\u6587\u672c");
        props.set("text", text);
        ObjectNode target = JsonNodeFactory.instance.objectNode();
        target.put("type", "string");
        target.put("description", "\u76ee\u6807\u8bed\u8a00\uff1azh/en/ja/ko/fr/de/es");
        props.set("target_lang", target);
        params.set("properties", props);
        ArrayNode req = JsonNodeFactory.instance.arrayNode();
        req.add("text");
        params.set("required", req);
        return params;
    }
    @Override public String execute(ObjectNode args) {
        String text = args.has("text") ? args.get("text").asText().trim() : "";
        String target = args.has("target_lang") ? args.get("target_lang").asText().trim() : "zh";
        if (text.isBlank()) return "\u8bf7\u63d0\u4f9b\u8981\u7ffb\u8bd1\u7684\u6587\u672c";
        try {
            ObjectNode body = mapper.createObjectNode();
            body.put("text", text);
            String url = API_URL + "?target_lang=" + URLEncoder.encode(target, StandardCharsets.UTF_8);
            String jsonBody = mapper.writeValueAsString(body);
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody)).build();
            HttpResponse<String> resp = httpClient().send(req, HttpResponse.BodyHandlers.ofString());
            JsonNode json = mapper.readTree(resp.body());
            if (json.has("data")) {
                JsonNode d = json.get("data");
                if (d.has("translated_text")) return d.get("translated_text").asText();
                if (d.has("translation")) return d.get("translation").asText();
            }
            if (json.has("translated_text")) return json.get("translated_text").asText();
            return "\u7ffb\u8bd1\u5931\u8d25";
        } catch (Exception e) {
            return "\u7ffb\u8bd1\u51fa\u9519: " + e.getMessage();
        }
    }
}
