package com.sekai.sekai_form.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sekai.sekai_form.agent.memory.ConversationMemoryService;
import com.sekai.sekai_form.agent.memory.MemoryContext;
import com.sekai.sekai_form.agent.memory.MemoryMessage;
import com.sekai.sekai_form.agent.memory.RagHit;
import com.sekai.sekai_form.agent.memory.RagService;
import com.sekai.sekai_form.dataobject.Live2DChatConfigDO;
import com.sekai.sekai_form.mapper.Live2DChatConfigMapper;
import com.sekai.sekai_form.security.AgentSecurityGuard;
import com.sekai.sekai_form.service.AgentSessionStateService;
import com.sekai.sekai_form.tool.ToolRegistry;
import com.sekai.sekai_form.tool.AgentTool;
import com.sekai.sekai_form.util.AgentSystemPromptBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** Application-level Agent facade. All model requests enter through this class. */
@Service
public class AgentService {
    private static final Logger logger = LoggerFactory.getLogger(AgentService.class);
    private final ObjectMapper objectMapper;
    private final Live2DChatConfigMapper configMapper;
    private final ConversationMemoryService memoryService;
    private final RagService ragService;
    private final AgentSessionStateService sessionStateService;
    private final ToolCallingService toolCallingService;
    private final ToolRegistry toolRegistry;
    private final AgentSystemPromptBuilder promptBuilder;
    private final AgentSecurityGuard securityGuard;
    private final long timeoutMs;
    private final java.util.concurrent.ExecutorService executionExecutor = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable, "agent-execution-worker");
        thread.setDaemon(true);
        return thread;
    });

    public AgentService(ObjectMapper objectMapper, Live2DChatConfigMapper configMapper,
                        ConversationMemoryService memoryService, RagService ragService,
                        AgentSessionStateService sessionStateService, ToolCallingService toolCallingService,
                        ToolRegistry toolRegistry, AgentSecurityGuard securityGuard,
                        List<AgentTool> agentTools,
                        @Value("${agent.timeout-ms:90000}") long timeoutMs) {
        this.objectMapper = objectMapper;
        this.configMapper = configMapper;
        this.memoryService = memoryService;
        this.ragService = ragService;
        this.sessionStateService = sessionStateService;
        this.toolCallingService = toolCallingService;
        this.toolRegistry = toolRegistry;
        this.promptBuilder = new AgentSystemPromptBuilder();
        this.securityGuard = securityGuard;
        if (agentTools != null) for (AgentTool agentTool : agentTools) toolRegistry.register(agentTool);
        this.timeoutMs = timeoutMs;
    }

    public AgentResult run(AgentRequest request) { return run(request, null); }

    /** Compatibility entry point matching the original Agent service contract. */
    public AgentResult runAgent(Long modelId, String conversationId, String userMessage) {
        return run(AgentRequest.builder().modelId(modelId).conversationId(conversationId).message(userMessage).build());
    }

    public AgentResult runAgent(AgentRequest request, AgentProgressCallback progressCallback) {
        return run(request, progressCallback);
    }

    public AgentResult run(AgentRequest request, AgentProgressCallback progressCallback) {
        if (request == null || request.getModelId() == null
                || (request.getMessage().isBlank() && request.getAttachments().isEmpty())) {
            return AgentResult.failure("请求内容不完整。", UUID.randomUUID().toString(), List.of());
        }
        final String traceId = UUID.randomUUID().toString();
        CompletableFuture<AgentResult> future = CompletableFuture.supplyAsync(() -> execute(request, traceId, progressCallback), executionExecutor);
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        java.util.concurrent.ScheduledFuture<?> progress = scheduler.schedule(
                () -> safeProgress(progressCallback, "progress", "任务仍在处理中，请稍候。"),
                Math.min(10000, Math.max(1000, timeoutMs / 4)), TimeUnit.MILLISECONDS);
        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException ex) {
            logger.warn("Agent execution timed out, traceId={}, timeoutMs={}", traceId, timeoutMs);
            future.cancel(true);
            safeProgress(progressCallback, "timeout", "处理超时，已安全停止。请稍后重试。");
            return AgentResult.failure("处理超时，已安全停止。请稍后重试。", traceId, List.of());
        } catch (Exception ex) {
            logger.error("Agent execution failed, traceId={}, type={}, message={}", traceId,
                    ex.getClass().getSimpleName(), securityGuard.clean(ex.getMessage()), ex);
            return AgentResult.failure("当前服务暂时不可用，请稍后再试。", traceId, List.of());
        } finally {
            progress.cancel(false);
            scheduler.shutdownNow();
        }
    }

    private AgentResult execute(AgentRequest request, String traceId, AgentProgressCallback callback) {
        Live2DChatConfigDO config = configMapper.getByModelId(request.getModelId());
        if (config == null || blank(config.getApiUrl()) || blank(config.getApiKey())) {
            return AgentResult.failure("AI 对话未配置，请在设置中配置 API。", traceId, List.of());
        }
        String conversationId = safeKey(request.getConversationId(), "anonymous");
        String userId = safeKey(request.getUserId(), "anonymous");
        sessionStateService.touch(conversationId, request.getSessionState());
        for (AgentAttachment attachment : request.getAttachments()) sessionStateService.putAttachment(conversationId, attachment);
        MemoryContext memory = memoryService.load(conversationId, userId, request.getSessionState());
        List<RagHit> rag = ragService.search(conversationId, userId, request.getMessage(), 4);
        List<String> relevantMemory = memoryService.relevantMemories(conversationId, request.getMessage(), 4);
        ArrayNode messages = buildMessages(request, config, memory, relevantMemory, rag);
        AgentExecutionContext context = new AgentExecutionContext(request, config, objectMapper, sessionStateService);
        AgentLoopResult loop = toolCallingService.run(context, messages, callback);
        String reply = securityGuard.clean(loop.reply());
        if (reply.isBlank()) reply = "暂时没有得到有效回复，请稍后再试。";
        if (!loop.success()) return AgentResult.failure(reply, traceId, loop.toolCalls());
        try {
            memoryService.saveTurn(conversationId, userId, request.getModelId(), request.getModality(), request.getMessage(), reply);
            if (isMemoryCandidate(request.getMessage(), reply)) {
                memoryService.saveMemory(conversationId, userId,
                        "用户：" + request.getMessage() + "\n助手：" + reply,
                        "conversation", 0.6);
            }
            if (reply.length() >= 20 || !loop.toolCalls().isEmpty()) {
                ragService.save(conversationId, userId, "Live2D 对话", request.getMessage() + "\n" + reply,
                        "modality=" + request.getModality());
            }
        } catch (Exception ignored) {
            // A memory backend outage must not turn a completed model response into a failure.
        }
        return AgentResult.successWithMedia(reply, loop.imageUrl(), loop.audioUrl(), traceId, loop.toolCalls());
    }

    private ArrayNode buildMessages(AgentRequest request, Live2DChatConfigDO config, MemoryContext memory,
                                    List<String> relevantMemory, List<RagHit> rag) {
        ArrayNode messages = objectMapper.createArrayNode();
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("AGENT_NAME", "Haru");
        variables.put("TASK_MODE", "AGENT");
        variables.put("CURRENT_TIME", ZonedDateTime.now(ZoneId.of("Asia/Shanghai"))
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z (EEEE)")));
        variables.put("CONVERSATION_ID", "当前会话（内部绑定，不向用户展示）");
        variables.put("USER_ID", "当前用户（内部绑定，不向用户展示）");
        variables.put("INPUT_MODALITY", request.getModality());
        variables.put("ATTACHMENTS", attachmentDescription(request));
        variables.put("SESSION_STATE", sessionStateService.describe(request.getConversationId()));
        variables.put("RECENT_MESSAGES", formatMessages(memory.recentMessages(), request.getExternalHistory()));
        variables.put("CONVERSATION_SUMMARY", blank(memory.summary()) ? "暂无滚动摘要" : securityGuard.clean(memory.summary()));
        variables.put("RELEVANT_MEMORY", relevantMemory.isEmpty() ? "暂无相关记忆" : securityGuard.clean(String.join("\n", relevantMemory)));
        variables.put("RAG_CONTEXT", rag.isEmpty() ? "暂无相关检索内容" : securityGuard.clean(formatRag(rag)));
        variables.put("AVAILABLE_TOOLS", describeTools());
        variables.put("MAX_TOOL_ITERATIONS", "5");
        variables.put("DOMAIN_NAME", "Live2D 虚拟角色陪伴与多模态交互");
        variables.put("DOMAIN_INTENTS", "角色闲聊、实时天气/时间/新闻、翻译、文件分析、语音转写与语音输出");
        variables.put("DOMAIN_TOOLS", describeTools());
        String prompt = promptBuilder.build(variables);
        if (!blank(config.getSystemPrompt())) prompt += "\n\n当前角色补充设定：\n" + securityGuard.clean(config.getSystemPrompt());
        messages.add(message("system", prompt));
        for (MemoryMessage item : memory.recentMessages()) messages.add(message(item.role(), securityGuard.clean(item.content())));
        appendExternalHistory(messages, request.getExternalHistory());
        messages.add(userMessage(request));
        return messages;
    }

    private ObjectNode userMessage(AgentRequest request) {
        ObjectNode message = objectMapper.createObjectNode();
        message.put("role", "user");
        if (request.getAttachments().stream().anyMatch(AgentAttachment::isImage)) {
            ArrayNode content = objectMapper.createArrayNode();
            ObjectNode text = objectMapper.createObjectNode();
            text.put("type", "text");
            text.put("text", securityGuard.clean(blank(request.getMessage()) ? "请结合图片处理用户请求" : request.getMessage()));
            content.add(text);
            for (AgentAttachment attachment : request.getAttachments()) {
                if (!attachment.isImage()) continue;
                ObjectNode image = objectMapper.createObjectNode(); image.put("type", "image_url");
                ObjectNode imageUrl = objectMapper.createObjectNode();
                imageUrl.put("url", "data:" + attachment.getMediaType() + ";base64," + java.util.Base64.getEncoder().encodeToString(attachment.getContent()));
                image.set("image_url", imageUrl); content.add(image);
            }
            message.set("content", content);
        } else message.put("content", securityGuard.clean(request.getMessage()));
        return message;
    }

    private void appendExternalHistory(ArrayNode messages, List<Map<String, Object>> history) {
        if (history == null) return;
        int start = Math.max(0, history.size() - 10);
        for (int i = start; i < history.size(); i++) {
            Map<String, Object> item = history.get(i);
            if (item == null || item.get("content") == null) continue;
            String content = item.get("content").toString().trim();
            String role = normalizeRole(item.get("role"));
            if (!content.isBlank() && !containsMessage(messages, role, content)) messages.add(message(role, content));
        }
    }

    private boolean containsMessage(ArrayNode messages, String role, String content) {
        for (JsonNode item : messages) {
            if (role.equals(item.path("role").asText()) && securityGuard.clean(content).equals(item.path("content").asText())) return true;
        }
        return false;
    }

    private String formatMessages(List<MemoryMessage> memory, List<Map<String, Object>> external) {
        List<String> values = new ArrayList<>();
        if (memory != null) for (MemoryMessage item : memory) values.add(item.role() + "：" + securityGuard.clean(item.content()));
        if (external != null) for (Map<String, Object> item : external) if (item != null && item.get("content") != null) values.add(normalizeRole(item.get("role")) + "：" + securityGuard.clean(item.get("content").toString()));
        if (values.isEmpty()) return "无历史对话";
        int start = Math.max(0, values.size() - 10);
        return String.join("\n", values.subList(start, values.size()));
    }

    private String formatRag(List<RagHit> hits) {
        StringBuilder result = new StringBuilder();
        for (RagHit hit : hits) result.append("【").append(hit.title()).append("】 ").append(hit.content()).append("\n");
        return result.toString().trim();
    }

    private String attachmentDescription(AgentRequest request) {
        if (request.getAttachments().isEmpty()) return "无附件";
        List<String> values = new ArrayList<>();
        for (AgentAttachment attachment : request.getAttachments()) values.add(attachment.getModality() + "附件（" + safeName(attachment.getName()) + "）");
        return String.join("、", values);
    }

    private String describeTools() {
        List<String> values = new ArrayList<>();
        for (JsonNode item : toolRegistry.getToolDefinitions()) values.add(item.path("function").path("name").asText() + "：" + item.path("function").path("description").asText());
        return values.isEmpty() ? "当前没有可用工具" : String.join("；", values);
    }

    private ObjectNode message(String role, String content) {
        ObjectNode message = objectMapper.createObjectNode();
        message.put("role", role);
        message.put("content", securityGuard.clean(content));
        return message;
    }

    private static String normalizeRole(Object value) { return "assistant".equals(value == null ? "" : value.toString()) ? "assistant" : "user"; }
    private static String safeName(String name) { return name == null || name.isBlank() ? "附件" : name.replaceAll("[\\\\/:*?\"<>|]", "_"); }
    private static String safeKey(String value, String fallback) { return value == null || value.isBlank() ? fallback : value.length() > 200 ? value.substring(0, 200) : value; }
    private static boolean blank(String value) { return value == null || value.isBlank(); }
    private static boolean isMemoryCandidate(String message, String reply) {
        if (blank(message) || blank(reply)) return false;
        return reply.length() >= 20 || message.contains("记住") || message.contains("偏好")
                || message.contains("喜欢") || message.contains("不要忘");
    }
    private static void safeProgress(AgentProgressCallback callback, String phase, String message) { if (callback != null) try { callback.onProgress(phase, message); } catch (Exception ignored) { } }
}
