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

public class JokeTool implements Tool {
    private static final String API_URL = "https://uapis.cn/api/v1/misc/joke";
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    
    @Override public String name() { return "tell_joke"; }
    
    @Override public String description() { 
        return "\u968f\u673a\u83b7\u53d6\u7b11\u8bdd\u6216\u6bb5\u5b50\u3002\u5f53\u7528\u6237\u8981\u6c42\u8bb2\u7b11\u8bdd\u3001\u8bb2\u6bb5\u5b50\u3001\u9017\u6211\u5f00\u5fc3\u65f6\u4f7f\u7528\u6b64\u5de5\u5177\u3002"; 
    }
    
    @Override
    public ObjectNode getParametersSchema() {
        ObjectNode params = JsonNodeFactory.instance.objectNode();
        params.put("type", "object");
        
        ObjectNode props = JsonNodeFactory.instance.objectNode();
        ObjectNode count = JsonNodeFactory.instance.objectNode();
        count.put("type", "integer");
        count.put("description", "\u7b11\u8bdd\u6570\u91cf\uff0c\u9ed8\u8ba41\u6761\uff0c\u6700\u591a3\u6761");
        props.set("count", count);
        params.set("properties", props);
        params.set("required", JsonNodeFactory.instance.arrayNode());
        
        return params;
    }
    
    @Override
    public String execute(ObjectNode arguments) {
        int count = Math.min(arguments.has("count") ? arguments.get("count").asInt() : 1, 3);
        if (count <= 0) count = 1;
        try {
            String url = API_URL + "?num=" + count;
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode resp = mapper.readTree(response.body());
            
            JsonNode jokes = resp.has("data") ? resp.get("data") : null;
            if (jokes == null || !jokes.isArray() || jokes.size() == 0) {
                return "\u4e3a\u4ec0\u4e48\u7a0b\u5e8f\u5458\u603b\u662f\u5206\u4e0d\u6e05\u4e07\u5723\u8282\u548c\u5723\u8bde\u8282\uff1f\n\u56e0\u4e3a Oct 31 == Dec 25\uff01\uff08\u516b\u8fdb\u523631\u7b49\u4e8e\u5341\u8fdb\u523625\uff09\ud83d\ude04";
            }
            
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < jokes.size(); i++) {
                if (i > 0) sb.append("\n---\n\n");
                JsonNode joke = jokes.get(i);
                String title = joke.has("title") ? joke.get("title").asText("") : "";
                String content = joke.has("content") ? joke.get("content").asText("") : "";
                if (!title.isEmpty()) sb.append(title).append("\n");
                sb.append(!content.isEmpty() ? content : (joke.has("text") ? joke.get("text").asText("") : ""));
            }
            String result = sb.toString().trim();
            return result.isEmpty() ? "\u4eca\u5929\u7b11\u8bdd\u5e93\u5b58\u544a\u6025\uff0c\u660e\u5929\u518d\u6765\u5427~ \ud83d\ude05" : result;
        } catch (Exception e) {
            return "\u7b11\u8bdd\u670d\u52a1\u6682\u65f6\u4e0d\u53ef\u7528\u3002\u8bb2\u4e2a\u7a0b\u5e8f\u5458\u7b11\u8bdd\u66ff\u8865\uff1a\n\u9762\u8bd5\u5b98\uff1a\u4f60\u671f\u671b\u7684\u85aa\u8d44\uff1f\n\u7a0b\u5e8f\u5458\uff1a5000\u3002\n\u9762\u8bd5\u5b98\uff1a\u7ed9\u4f608000\u3002\n\u7a0b\u5e8f\u5458\uff1a\u4e0d\u884c\uff0c\u5c31\u89815000\uff01\n\n\ud83d\ude04";
        }
    }
}