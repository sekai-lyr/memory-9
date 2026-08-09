package com.sekai.sekai_form.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sekai.sekai_form.dataobject.Live2DChatConfigDO;
import com.sekai.sekai_form.dataobject.Live2DDialogueDO;
import com.sekai.sekai_form.dataobject.Live2DModelDO;
import com.sekai.sekai_form.mapper.Live2DChatConfigMapper;
import com.sekai.sekai_form.mapper.Live2DDialogueMapper;
import com.sekai.sekai_form.mapper.Live2DModelMapper;
import com.sekai.sekai_form.model.ChatResponse;
import com.sekai.sekai_form.model.Result;
import com.sekai.sekai_form.service.Live2DService;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import com.sekai.sekai_form.tool.*;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class Live2DServiceImpl implements Live2DService {
    private final Live2DModelMapper modelMapper;
    private final Live2DDialogueMapper dialogueMapper;
    private final Live2DChatConfigMapper chatConfigMapper;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private static final Path MODELS_DIR = Paths.get("models", "2d");
    private final ToolRegistry toolRegistry = new ToolRegistry();

    public Live2DServiceImpl(Live2DModelMapper modelMapper, Live2DDialogueMapper dialogueMapper,
                             Live2DChatConfigMapper chatConfigMapper) {
        this.modelMapper = modelMapper;
        this.dialogueMapper = dialogueMapper;
        this.chatConfigMapper = chatConfigMapper;
        this.restTemplate = new RestTemplate();
        // Force UTF-8 for all message converters
        this.restTemplate.getMessageConverters().forEach(converter -> {
            if (converter instanceof org.springframework.http.converter.StringHttpMessageConverter) {
                ((org.springframework.http.converter.StringHttpMessageConverter) converter).setDefaultCharset(java.nio.charset.StandardCharsets.UTF_8);
            }
        });
        this.objectMapper = new ObjectMapper();
        try { Files.createDirectories(MODELS_DIR); } catch (IOException ignored) {}
        // Register 7.24.pm function calling tools
        toolRegistry.register(new WeatherTool());
        toolRegistry.register(new JokeTool());
        toolRegistry.register(new CurrentTimeTool());
        toolRegistry.register(new TranslateTool());
        toolRegistry.register(new NewsTool());
        toolRegistry.register(new ConstellationTool());
    }

    @Override
    public Result<List<Live2DModelDO>> listModels() { return Result.ok(modelMapper.listAll()); }

    @Override
    public Result<Live2DModelDO> getModel(Long id) {
        Live2DModelDO m = modelMapper.findById(id);
        if (m == null) return Result.fail("模型不存在");
        return Result.ok(m);
    }

    @Override
    public Result<Live2DModelDO> uploadModel(MultipartFile file, String displayName) {
        try {
            String modelName = file.getOriginalFilename();
            if (modelName == null || !modelName.endsWith(".zip")) return Result.fail("请上传zip文件");
            String baseName = modelName.replace(".zip", "");
            Path targetDir = MODELS_DIR.resolve(baseName);
            Files.createDirectories(targetDir);
            try (ZipInputStream zis = new ZipInputStream(file.getInputStream())) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    Path outPath = targetDir.resolve(entry.getName()).normalize();
                    if (!outPath.startsWith(targetDir)) continue;
                    if (entry.isDirectory()) { Files.createDirectories(outPath); continue; }
                    Files.createDirectories(outPath.getParent());
                    try (OutputStream os = Files.newOutputStream(outPath)) { zis.transferTo(os); }
                }
            }
            Live2DModelDO model = new Live2DModelDO();
            model.setModelName(baseName); model.setModelPath(targetDir.toString()); model.setDisplayName(displayName);
            modelMapper.insert(model);
            return Result.ok("上传成功", model);
        } catch (IOException e) { return Result.fail("上传失败: " + e.getMessage()); }
    }

    @Override
    public Result<Void> deleteModel(Long id) {
        Live2DModelDO m = modelMapper.findById(id);
        if (m != null) {
            try { Files.walk(Paths.get(m.getModelPath())).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete); } catch (IOException ignored) {}
        }
        modelMapper.deleteById(id);
        return Result.ok("删除成功", null);
    }

    @Override
    public Result<List<Live2DDialogueDO>> listDialogues(Long modelId) { return Result.ok(dialogueMapper.listByModelId(modelId)); }

    @Override
    public Result<Live2DDialogueDO> getRandomDialogue(Long modelId, String category) {
        List<Live2DDialogueDO> list = dialogueMapper.listByCategory(modelId, category);
        if (list == null || list.isEmpty()) return Result.fail("无对话");
        return Result.ok(list.get(new Random().nextInt(list.size())));
    }

    @Override
    public Result<ChatResponse> chat(Long modelId, String message, List<Map<String, Object>> history) {
        Live2DChatConfigDO config = chatConfigMapper.getByModelId(modelId);
        if (config == null || config.getApiUrl() == null || config.getApiUrl().isBlank()) {
            return Result.fail("AI?????????????API");
        }
        try {
            // Always use the comprehensive Haru system prompt
            String sysPrompt = getDefaultSystemPrompt();
            // Append any custom DB prompt as additional context
            String dbPrompt = config.getSystemPrompt();
            if (dbPrompt != null && dbPrompt.length() > 20 && !isGarbled(dbPrompt) && !dbPrompt.equals(sysPrompt)) {
                sysPrompt = sysPrompt + "\n\n[Additional context: " + dbPrompt + "]";
            }
            // Add time context
            var h = java.time.LocalTime.now().getHour();
            sysPrompt += "\n[Time: " + h + ":00]";
            
            // Build messages array
            ArrayNode messages = objectMapper.createArrayNode();
            messages.add(chatMessage("system", sysPrompt));
            
            // Emotion hint
            String emotionHint = detectEmotion(message);
            if (!emotionHint.isEmpty()) {
                messages.add(chatMessage("system", emotionHint));
            }
            
            // History
            if (history != null) {
                int start = Math.max(0, history.size() - 10);
                for (int i = start; i < history.size(); i++) {
                    Map<String, Object> hm = history.get(i);
                    String role = hm.get("role") != null ? hm.get("role").toString() : "user";
                    String cnt = hm.get("content") != null ? hm.get("content").toString() : "";
                    if (!cnt.isBlank()) messages.add(chatMessage(role, cnt));
                }
            }
            
            // === 7.24.pm Intent Detection & Direct Tool Calling ===
            String preToolResult = null;
            String lowerMsg = message.toLowerCase();
            boolean isWeather = lowerMsg.contains("天气") || lowerMsg.contains("weather") 
                             || lowerMsg.contains("温度") || lowerMsg.contains("下雨")
                             || lowerMsg.contains("热") && lowerMsg.contains("不") == false;
            boolean isJoke = lowerMsg.contains("笑话") || lowerMsg.contains("joke")
                          || lowerMsg.contains("搞笑") || lowerMsg.contains("讲个");
            boolean isTranslate = lowerMsg.contains("翻译") || lowerMsg.contains("translate")
                               || message.contains("英文") || message.contains("日文")
                               || message.contains("韩文") || message.contains("法文");
            boolean isNews = lowerMsg.contains("新闻") || lowerMsg.contains("热搜")
                          || lowerMsg.contains("热点") || lowerMsg.contains("news");
            boolean isConstellation = message.contains("星座") || message.contains("运势")
                                   || message.contains("白羊") || message.contains("金牛")
                                   || message.contains("双子") || message.contains("巨蟹")
                                   || message.contains("狮子") || message.contains("处女")
                                   || message.contains("天秤") || message.contains("天蝎")
                                   || message.contains("射手") || message.contains("摩羯")
                                   || message.contains("水瓶") || message.contains("双鱼");
            boolean isTime = lowerMsg.contains("几点") || lowerMsg.contains("时间")
                          || lowerMsg.contains("日期") || lowerMsg.contains("星期几")
                          || lowerMsg.contains("今天几号");
            
            System.out.println("[Chat] message=" + message + " isWeather=" + isWeather + " isJoke=" + isJoke);
            if (isWeather) {
                // Extract city from message
                String city = extractCity(message);
                try {
                    com.fasterxml.jackson.databind.node.ObjectNode args = objectMapper.createObjectNode();
                    args.put("city", city);
                    Tool wt = toolRegistry.get("get_weather");
                    if (wt != null) { preToolResult = wt.execute(args); System.out.println("[Chat] Weather result: " + (preToolResult != null ? preToolResult.substring(0, Math.min(80, preToolResult.length())) : "null")); }
                } catch (Exception e) { System.err.println("[Chat] Tool pre-execution error: " + e.getMessage()); e.printStackTrace(); }
            } else if (isJoke) {
                try {
                    com.fasterxml.jackson.databind.node.ObjectNode args = objectMapper.createObjectNode();
                    args.put("count", 1);
                    Tool jt = toolRegistry.get("tell_joke");
                    if (jt != null) preToolResult = jt.execute(args);
                } catch (Exception e) { System.err.println("[Chat] Tool pre-execution error: " + e.getMessage()); e.printStackTrace(); }
            } else if (isTranslate) {
                try {
                    com.fasterxml.jackson.databind.node.ObjectNode args = objectMapper.createObjectNode();
                    args.put("text", message);
                    args.put("target_lang", message.contains("英文") || message.contains("english") ? "en" : message.contains("日文") ? "ja" : message.contains("韩文") ? "ko" : "zh");
                    Tool tt = toolRegistry.get("translate");
                    if (tt != null) preToolResult = tt.execute(args);
                } catch (Exception e) { System.err.println("[Chat] Translate error: " + e.getMessage()); }
            } else if (isNews) {
                try {
                    com.fasterxml.jackson.databind.node.ObjectNode args = objectMapper.createObjectNode();
                    args.put("type", "baidu");
                    args.put("count", 5);
                    Tool nt = toolRegistry.get("get_news");
                    if (nt != null) preToolResult = nt.execute(args);
                } catch (Exception e) { System.err.println("[Chat] News error: " + e.getMessage()); }
            } else if (isConstellation) {
                try {
                    com.fasterxml.jackson.databind.node.ObjectNode args = objectMapper.createObjectNode();
                    String[] signs = {"白羊","金牛","双子","巨蟹","狮子","处女","天秤","天蝎","射手","摩羯","水瓶","双鱼"};
                    String found = "白羊座";
                    for (String s : signs) { if (message.contains(s)) { found = s + "座"; break; } }
                    args.put("constellation", found);
                    Tool ct = toolRegistry.get("get_constellation");
                    if (ct != null) preToolResult = ct.execute(args);
                } catch (Exception e) { System.err.println("[Chat] Constellation error: " + e.getMessage()); }
            } else if (isTime) {
                try {
                    com.fasterxml.jackson.databind.node.ObjectNode args = objectMapper.createObjectNode();
                    args.put("timezone", "Asia/Shanghai");
                    Tool timet = toolRegistry.get("get_current_time");
                    if (timet != null) preToolResult = timet.execute(args);
                } catch (Exception e) { System.err.println("[Chat] Tool pre-execution error: " + e.getMessage()); e.printStackTrace(); }
            }
            
            // If tool was called, inject result as system context
            if (preToolResult != null && !preToolResult.isBlank()) {
                message = message + "\n\n【系统已查询到以下信息，请直接用这些数据回复用户，不要再说“我帮你查查”之类的话】\n" + preToolResult;
            }

            messages.add(chatMessage("user", message));
            
            // === 7.24.pm Function Calling Loop ===
            String model = config.getModelName() != null ? config.getModelName() : "deepseek-chat";
            String apiUrl = config.getApiUrl();
            String apiKey = config.getApiKey();
            int maxTokens = config.getMaxTokens() != null ? config.getMaxTokens() : 1024;
            double temp = config.getTemperature() != null ? config.getTemperature() : 0.85;
            
            String finalReply = "";
            int maxIterations = 5;
            
            for (int iter = 0; iter < maxIterations; iter++) {
                ObjectNode body = objectMapper.createObjectNode();
                body.put("model", model);
                body.put("temperature", temp);
                body.put("max_tokens", maxTokens);
                body.set("messages", messages);
                
                // Include tool definitions
                if (!toolRegistry.isEmpty()) {
                    body.set("tools", toolRegistry.getToolDefinitions());
                    body.put("tool_choice", "auto");
                }
                
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setBearerAuth(apiKey);
                
                HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);
                ResponseEntity<String> resp = restTemplate.exchange(apiUrl, HttpMethod.POST, entity, String.class);
                
                JsonNode respJson = objectMapper.readTree(resp.getBody());
                JsonNode choice = respJson.path("choices").get(0);
                JsonNode msg = choice.path("message");
                
                String content = msg.has("content") && !msg.get("content").isNull() 
                    ? msg.get("content").asText().trim() : "";
                
                if (!content.isEmpty() && finalReply.isEmpty()) {
                    finalReply = content;
                }
                
                // Check for tool_calls
                if (!msg.has("tool_calls") || msg.get("tool_calls").size() == 0) {
                    if (finalReply.isEmpty() && !content.isEmpty()) finalReply = content;
                    if (finalReply.isEmpty()) finalReply = content;
                    break;
                }
                
                // Add assistant message with tool_calls
                messages.add(msg);
                
                // Process each tool call
                JsonNode toolCalls = msg.get("tool_calls");
                for (JsonNode tc : toolCalls) {
                    String callId = tc.get("id").asText();
                    JsonNode func = tc.get("function");
                    String funcName = func.get("name").asText();
                    String argsStr = func.has("arguments") ? func.get("arguments").asText() : "{}";
                    
                    Tool tool = toolRegistry.get(funcName);
                    String toolResult;
                    if (tool == null) {
                        toolResult = "Error: unknown tool " + funcName;
                    } else {
                        try {
                            ObjectNode args = (ObjectNode) objectMapper.readTree(argsStr);
                            toolResult = tool.execute(args);
                        } catch (Exception e) {
                            toolResult = "Tool error: " + e.getMessage();
                        }
                    }
                    
                    // Add tool result message
                    ObjectNode toolMsg = objectMapper.createObjectNode();
                    toolMsg.put("role", "tool");
                    toolMsg.put("tool_call_id", callId);
                    toolMsg.put("content", toolResult);
                    messages.add(toolMsg);
                }
            }
            
            if (!finalReply.isBlank()) {
                String motion = finalReply.contains("?") || finalReply.contains("!") || finalReply.contains("！") ? "Tap" : "Idle";
                return Result.ok(new ChatResponse(finalReply, motion));
            }
            return Result.fail("AI????");
        } catch (Exception e) {
            return Result.fail("AI????: " + e.getMessage());
        }
    }

    private String detectEmotion(String msg) {
        if (msg == null) return "";
        if (msg.contains("累") || msg.contains("辛苦") || msg.contains("疲惫"))
            return "User is tired. Give warm, specific comfort with concrete suggestions. Be caring and detailed.";
        if (msg.contains("开心") || msg.contains("高兴") || msg.contains("快乐"))
            return "User is happy. Celebrate enthusiastically, ask for details to share their joy.";
        if (msg.contains("难过") || msg.contains("伤心") || msg.contains("哭"))
            return "User is sad. Empathize deeply first, then gently offer comfort. Don't minimize their feelings.";
        if (msg.contains("无聊") || msg.contains("没意思"))
            return "User is bored. Be creative - suggest a fun topic, tell a story, or play a word game.";
        return "";
    }
    
    /** Check if a Chinese prompt has been garbled (contains no valid CJK characters despite claiming to be Chinese) */
    private boolean isGarbled(String text) {
        if (text == null) return true;
        // If text contains Chinese claim markers but no actual Chinese chars, it's garbled
        boolean claimsChinese = text.contains("Sekai") || text.contains("Live2D") || text.contains("Haru") 
                             || text.contains("看板") || text.contains("你是");
        if (claimsChinese) {
            // Count actual CJK characters
            long cjkCount = text.codePoints()
                .filter(cp -> java.lang.Character.UnicodeScript.of(cp) == java.lang.Character.UnicodeScript.HAN)
                .count();
            return cjkCount < 5; // Less than 5 Chinese chars means garbled
        }
        return false;
    }

        private String getDefaultSystemPrompt() {
        return "你叫Haru，是一个温暖的Live2D看板娘。回答要准确、温暖、具体，不要短小。\n\n" +
               "你有以下工具：\n" +
               "get_weather（查天气）、tell_joke（讲笑话）\n\n" +
               "【工具选择规则】\n" +
               "- 用户询问天气、温度、下雨、热不热 -> 必须调用 get_weather\n" +
               "- 用户要求讲笑话、搞笑 -> 调用 tell_joke\n\n" +
               "【回复要求】\n" +
               "- 每次回复至少4句话，150字以上\n" +
               "- 天气回复格式：城市名+天气+温度+风向+风力+湿度+发布时间+温馨提示\n" +
               "- 说话自然亲切，像微信朋友，带可爱口癖（~、呢、嘞）\n" +
               "- 结合当前时间给合适关心\n" +
               "- 用户问时间、日期 -> 调用 get_current_time\n" +
               "- 用户要翻译 -> 调用 translate\n" +
               "- 用户问新闻、热搜 -> 调用 get_news\n" +
               "- 用户问星座运势 -> 调用 get_constellation";
    }
    private ObjectNode chatMessage(String role, String content) {
        ObjectNode msg = objectMapper.createObjectNode();
        msg.put("role", role);
        msg.put("content", content);
        return msg;
    }

    @Override
    public Result<Live2DChatConfigDO> getChatConfig(Long modelId) {
        Live2DChatConfigDO config = chatConfigMapper.getByModelId(modelId);
        if (config == null) return Result.fail("聊天配置不存在");
        return Result.ok(config);
    }

    @Override
    public Result<Void> saveChatConfig(Live2DChatConfigDO config) {
        Live2DChatConfigDO existing = chatConfigMapper.getByModelId(config.getModelId());
        if (existing != null) {
            chatConfigMapper.update(config);
        } else {
            chatConfigMapper.insert(config);
        }
        return Result.ok("保存成功", null);
    }

    // ==================== 图片识别 ====================

    @Override
    public Result<ChatResponse> recognizeImage(Long modelId, MultipartFile image, String question) {
        Live2DChatConfigDO config = chatConfigMapper.getByModelId(modelId);
        if (config == null || config.getApiKey() == null || config.getApiKey().isBlank()) {
            return Result.fail("AI对话未配置，请在设置中配置API");
        }
        try {
            byte[] imageBytes = image.getBytes();
            String base64Image = java.util.Base64.getEncoder().encodeToString(imageBytes);
            String dataUri = "data:" + (image.getContentType() != null ? image.getContentType() : "image/jpeg") + ";base64," + base64Image;

            ObjectNode imageUrlObj = objectMapper.createObjectNode();
            imageUrlObj.put("url", dataUri);

            ObjectNode imagePart = objectMapper.createObjectNode();
            imagePart.put("type", "image_url");
            imagePart.set("image_url", imageUrlObj);

            ObjectNode textPart = objectMapper.createObjectNode();
            textPart.put("type", "text");
            textPart.put("text", question != null && !question.isBlank() ? question : "请描述这张图片");

            ArrayNode contentArray = objectMapper.createArrayNode();
            contentArray.add(textPart);
            contentArray.add(imagePart);

            ObjectNode userMsg = objectMapper.createObjectNode();
            userMsg.put("role", "user");
            userMsg.set("content", contentArray);

            ArrayNode messages = objectMapper.createArrayNode();
            messages.add(userMsg);

            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", config.getModelName() != null ? config.getModelName() : "deepseek-chat");
            body.set("messages", messages);
            body.put("max_tokens", config.getMaxTokens() != null ? config.getMaxTokens() : 1024);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (config.getApiKey() != null && !config.getApiKey().isBlank()) {
                headers.setBearerAuth(config.getApiKey());
            }

            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);
            ResponseEntity<String> resp = restTemplate.exchange(config.getApiUrl(), HttpMethod.POST, entity, String.class);

            JsonNode respJson = objectMapper.readTree(resp.getBody());
            String reply = respJson.path("choices").get(0).path("message").path("content").asText().trim();

            ChatResponse chatResp = new ChatResponse(reply, "Tap");
            return Result.ok(chatResp);
        } catch (Exception e) {
            return Result.fail("图片识别失败: " + e.getMessage());
        }
    }

    // ==================== 图片生成 ====================

    @Override
    public Result<ChatResponse> generateImage(Long modelId, String prompt) {
        Live2DChatConfigDO config = chatConfigMapper.getByModelId(modelId);
        if (config == null || config.getApiKey() == null || config.getApiKey().isBlank()) {
            return Result.fail("AI对话未配置，请在设置中配置API");
        }
        try {
            String imageModel = config.getImageModelName() != null ? config.getImageModelName() : "wanx2.1-t2i-turbo";
            String imageApiUrl = config.getImageApiUrl();
            if (imageApiUrl == null || imageApiUrl.isBlank()) {
                String base = config.getApiUrl() != null ? config.getApiUrl() : "";
                imageApiUrl = base.replace("/compatible-mode/v1", "").replace("/compatible-mode", "") + "/api/v1";
            }

            ObjectNode input = objectMapper.createObjectNode();
            input.put("prompt", prompt);

            ObjectNode parameters = objectMapper.createObjectNode();
            parameters.put("size", "1024*1024");
            parameters.put("n", 1);

            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", imageModel);
            body.set("input", input);
            body.set("parameters", parameters);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(config.getApiKey());
            headers.set("X-DashScope-Async", "enable");

            String submitUrl = imageApiUrl.replaceAll("/$", "") + "/services/aigc/text2image/image-synthesis";
            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);
            ResponseEntity<String> createResp = restTemplate.exchange(submitUrl, HttpMethod.POST, entity, String.class);

            JsonNode createJson = objectMapper.readTree(createResp.getBody());
            String taskId = createJson.path("output").path("task_id").asText();

            for (int i = 0; i < 90; i++) {
                Thread.sleep(2000);
                String taskUrl = imageApiUrl.replaceAll("/$", "") + "/tasks/" + taskId;
                ResponseEntity<String> taskResp = restTemplate.exchange(taskUrl, HttpMethod.GET, null, String.class);
                JsonNode taskJson = objectMapper.readTree(taskResp.getBody());
                String status = taskJson.path("output").path("task_status").asText();

                if ("SUCCEEDED".equals(status)) {
                    String imageUrl = taskJson.path("output").path("results").get(0).path("url").asText();
                    ChatResponse chatResp = new ChatResponse("图片已生成~", "Idle");
                    chatResp.setImageUrl(imageUrl);
                    return Result.ok(chatResp);
                }
                if ("FAILED".equals(status)) {
                    return Result.fail("图片生成失败: " + taskJson.path("output").toString());
                }
            }
            return Result.fail("图片生成超时");
        } catch (Exception e) {
            return Result.fail("图片生成失败: " + e.getMessage());
        }
    }
    /** Extract city name from message - 7.24.pm pattern */
    private String extractCity(String text) {
        if (text == null) return "北京";
        String[] cities = {"杭州", "北京", "上海", "广州", "深圳", 
                          "成都", "重庆", "武汉", "南京", "西安",
                          "长沙", "郑州", "天津", "苏州", "沈阳",
                          "青岛", "大连", "厦门", "济南", "昆明"};
        for (String city : cities) {
            if (text.contains(city)) return city;
        }
        // Try to extract anything that looks like a city name (2-3 chars before weather-related words)
        String clean = text.replaceAll("[天气温度怎么样如何今天明天查询呢吗呀嘞啦哦嘞]", "");
        clean = clean.replaceAll("[?\uff1f!\uff01.。,，\s]+", "").trim();
        if (clean.length() >= 2 && clean.length() <= 4) return clean;
        return "北京";
    }
}