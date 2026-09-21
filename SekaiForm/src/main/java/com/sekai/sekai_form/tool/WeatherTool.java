package com.sekai.sekai_form.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class WeatherTool implements Tool {
    private static final String API_URL = "https://uapis.cn/api/v1/misc/weather";
    private static final ObjectMapper mapper = new ObjectMapper();
    private static HttpClient httpClient() { return HttpClient.newHttpClient(); }
    
    @Override public String name() { return "get_weather"; }
    
    @Override public String description() { 
        return "\u67e5\u8be2\u6307\u5b9a\u57ce\u5e02\u7684\u5b9e\u65f6\u5929\u6c14\u4fe1\u606f\uff0c\u5305\u62ec\u5929\u6c14\u72b6\u51b5\u3001\u6e29\u5ea6\u3001\u98ce\u5411\u3001\u98ce\u529b\u3001\u6e7f\u5ea6\u7b49"; 
    }
    
    @Override
    public ObjectNode getParametersSchema() {
        ObjectNode params = JsonNodeFactory.instance.objectNode();
        params.put("type", "object");
        
        ObjectNode props = JsonNodeFactory.instance.objectNode();
        ObjectNode city = JsonNodeFactory.instance.objectNode();
        city.put("type", "string");
        city.put("description", "\u57ce\u5e02\u540d\u79f0\uff0c\u4f8b\u5982\uff1a\u5317\u4eac\u3001\u4e0a\u6d77\u3001\u676d\u5dde");
        props.set("city", city);
        params.set("properties", props);
        
        ArrayNode required = JsonNodeFactory.instance.arrayNode();
        required.add("city");
        params.set("required", required);
        
        return params;
    }
    
    @Override
    public String execute(ObjectNode arguments) {
        String city = arguments.has("city") ? arguments.get("city").asText() : "\u5317\u4eac";
        try {
            String encoded = URLEncoder.encode(city, StandardCharsets.UTF_8);
            String url = API_URL + "?city=" + encoded;
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
            
            HttpResponse<String> response = httpClient().send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();
            JsonNode json = mapper.readTree(body);
            
            // uapis.cn API returns flat JSON without code/data wrapper
            String weather = json.has("weather") ? json.get("weather").asText() : "\u672a\u77e5";
            String temp = json.has("temperature") ? String.valueOf(json.get("temperature").asInt()) : "\u672a\u77e5";
            String windDir = json.has("wind_direction") ? json.get("wind_direction").asText() : "\u672a\u77e5";
            String windPower = json.has("wind_power") ? json.get("wind_power").asText() : "\u672a\u77e5";
            String humidity = json.has("humidity") ? String.valueOf(json.get("humidity").asInt()) : "\u672a\u77e5";
            String reportTime = json.has("report_time") ? json.get("report_time").asText() : "";
            
            String tip = "";
            int tempVal = json.has("temperature") ? json.get("temperature").asInt() : 0;
            if (tempVal >= 35) tip = "\u5929\u6c14\u708e\u70ed\uff0c\u8bf7\u6ce8\u610f\u9632\u6652\u548c\u8865\u6c34\u54e6\uff01";
            else if (tempVal <= 10) tip = "\u5929\u6c14\u8f83\u51b7\uff0c\u8bb0\u5f97\u6dfb\u8863\u4fdd\u6696\u54e6~";
            
            return String.format(
                "%s\u5f53\u524d\u7684\u5929\u6c14\u60c5\u51b5\u5982\u4e0b\uff1a\n- \u5929\u6c14\uff1a%s\n- \u6e29\u5ea6\uff1a%s\u00b0C\n- \u98ce\u5411\uff1a%s\n- \u98ce\u529b\uff1a%s\n- \u6e7f\u5ea6\uff1a%s%%\n\n%s%s",
                city, weather, temp, windDir, windPower, humidity,
                reportTime.isEmpty() ? "" : "\u6570\u636e\u662f" + reportTime + "\u7684\uff0c",
                tip
            );
        } catch (Exception e) {
            return "\u5929\u6c14\u67e5\u8be2\u5931\u8d25\uff1a" + e.getMessage();
        }
    }
}
