package com.sekai.sekai_form.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sekai.sekai_form.security.AgentSecurityGuard;
import com.sekai.sekai_form.tool.ToolRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** The only service allowed to turn a model response into tool execution. */
@Service
public class ToolCallingService {
    private static final Logger logger = LoggerFactory.getLogger(ToolCallingService.class);
    private static final int MAX_ITERATIONS = 5;
    private static final int MAX_DISTINCT_TOOLS = 3;
    private static final int MAX_RETRIES = 1;
    private static final int TOOL_TIMEOUT_SECONDS = 75;
    private final AgentModelClient modelClient;
    private final ToolRegistry registry;
    private final AgentSecurityGuard securityGuard;
    private final ExecutorService toolExecutor = Executors.newFixedThreadPool(4, runnable -> {
        Thread thread = new Thread(runnable, "agent-tool-worker");
        thread.setDaemon(true);
        return thread;
    });

    public ToolCallingService(AgentModelClient modelClient, ToolRegistry registry, AgentSecurityGuard securityGuard) {
        this.modelClient = modelClient;
        this.registry = registry;
        this.securityGuard = securityGuard;
    }

    public AgentLoopResult run(AgentExecutionContext context, ArrayNode messages, AgentProgressCallback callback) {
        ArrayNode availableTools = toolsFor(context.getRequest().getForcedToolNames());
        AgentPlan plan = createPlan(context.getRequest());
        List<ToolCallResult> callResults = new ArrayList<>();
        Set<String> usedTools = new LinkedHashSet<>();
        String imageUrl = null;
        String audioUrl = null;
        String finalReply = "";
        int modelRetries = 0;
        String lastModelFailure = "当前服务暂时不可用，请稍后再试。";

        notify(callback, "planning", "已完成任务规划，开始执行");
        for (int iteration = 1; iteration <= MAX_ITERATIONS; iteration++) {
            plan.markIteration(iteration);
            notify(callback, "reasoning", "正在理解第 " + iteration + " 轮结果");
            JsonNode response;
            try {
                response = modelClient.complete(context.getConfig(), messages, availableTools);
                modelRetries = 0;
            } catch (Exception ex) {
                lastModelFailure = safeModelFailure(ex.getMessage());
                logger.warn("Model call failed at iteration={}, retry={}/{}, type={}, message={}",
                        iteration, modelRetries, MAX_RETRIES, ex.getClass().getSimpleName(), ex.getMessage());
                if (modelRetries++ < MAX_RETRIES) {
                    notify(callback, "retry", "模型服务暂时失败，正在重试");
                    iteration--;
                    continue;
                }
                return new AgentLoopResult(false, lastModelFailure, imageUrl, audioUrl, callResults);
            }

            JsonNode assistant = ToolCall.assistantMessage(response);
            List<ToolCall> toolCalls = ToolCall.parse(response, context.getObjectMapper());
            if (toolCalls.isEmpty()) {
                String content = assistant == null ? "" : assistant.path("content").asText("").trim();
                if (mustForceTool(context.getRequest(), usedTools)) {
                    ToolCall forced = forcedCall(context, firstMissingForced(context.getRequest(), usedTools));
                    if (forced != null) {
                        notify(callback, "tool", "正在执行所需的多模态工具");
                        List<ToolCallResult> forcedResults = executeCalls(context, List.of(forced), messages, usedTools);
                        callResults.addAll(forcedResults);
                        appendToolMessages(context, messages, List.of(forced), forcedResults);
                        imageUrl = mediaUrl(forcedResults, "imageUrl", imageUrl);
                        audioUrl = mediaUrl(forcedResults, "audioUrl", audioUrl);
                        plan.replan("模型未显式调用强制能力，已按入口契约补全");
                        continue;
                    }
                }
                finalReply = content;
                break;
            }

            if (assistant != null && assistant.isObject()) messages.add(assistant.deepCopy());
            List<ToolCall> accepted = new ArrayList<>();
            for (ToolCall call : toolCalls) {
                if (registry.get(call.getName()) == null && registry.getAgentTool(call.getName()) == null) {
                    accepted.add(new ToolCall(call.getId(), call.getName(), call.getArguments()));
                } else {
                    usedTools.add(registry.canonicalName(call.getName()));
                    accepted.add(call);
                }
            }
            if (usedTools.size() > MAX_DISTINCT_TOOLS) return new AgentLoopResult(false, "本次请求包含过多工具步骤，已安全停止。", imageUrl, audioUrl, callResults);
            notify(callback, "tool", "正在执行 " + accepted.size() + " 个工具步骤");
            List<ToolCallResult> results = executeCalls(context, accepted, messages, usedTools);
            callResults.addAll(results);
            imageUrl = mediaUrl(results, "imageUrl", imageUrl);
            audioUrl = mediaUrl(results, "audioUrl", audioUrl);
            plan.replan("工具结果已返回，重新判断下一步");
            appendToolMessages(context, messages, accepted, results);
        }
        if (finalReply.isBlank()) finalReply = callResults.isEmpty() ? "暂时没有得到有效回复，请稍后再试。" : "处理完成，请查看上面的结果。";
        return new AgentLoopResult(true, securityGuard.clean(finalReply), imageUrl, audioUrl, callResults);
    }

    private List<ToolCallResult> executeCalls(AgentExecutionContext context, List<ToolCall> calls,
                                               ArrayNode messages, Set<String> usedTools) {
        List<CompletableFuture<ToolCallResult>> futures = new ArrayList<>();
        for (ToolCall call : calls) futures.add(CompletableFuture.supplyAsync(() -> executeOne(context, call), toolExecutor));
        List<ToolCallResult> results = new ArrayList<>();
        for (int i = 0; i < futures.size(); i++) {
            CompletableFuture<ToolCallResult> future = futures.get(i);
            try { results.add(future.get(TOOL_TIMEOUT_SECONDS, TimeUnit.SECONDS)); }
            catch (Exception ex) { results.add(new ToolCallResult(calls.get(i).getName(), false, 1, 0, "工具执行超时")); }
        }
        return results;
    }

    private ToolCallResult executeOne(AgentExecutionContext context, ToolCall call) {
        long started = System.nanoTime();
        ObjectNode args = securityGuard.cleanArguments(call.getArguments());
        if (args.has("_invalid_arguments")) return new ToolCallResult(call.getName(), false, 1, elapsed(started), "工具参数格式无效");
        if (registry.get(call.getName()) == null && registry.getAgentTool(call.getName()) == null) {
            return new ToolCallResult(call.getName(), false, 1, elapsed(started), "工具不可用");
        }
        String validationError = registry.validateArguments(call.getName(), args);
        if (validationError != null) return new ToolCallResult(call.getName(), false, 1, elapsed(started), validationError);
        String output = "";
        int attempt = 0;
        while (attempt <= MAX_RETRIES) {
            attempt++;
            try {
                com.sekai.sekai_form.agent.ToolResult<?> result = registry.execute(call.getName(), args, context);
                output = securityGuard.clean(stringify(result));
                if (result.isSuccess() || !result.isRetryable() || attempt > MAX_RETRIES) {
                    return new ToolCallResult(call.getName(), result.isSuccess(), attempt, elapsed(started), output);
                }
            } catch (Exception ex) {
                output = "工具暂时失败";
                if (attempt > MAX_RETRIES) return new ToolCallResult(call.getName(), false, attempt, elapsed(started), output);
            }
        }
        return new ToolCallResult(call.getName(), false, attempt, elapsed(started), output);
    }

    private void appendToolMessages(AgentExecutionContext context, ArrayNode messages, List<ToolCall> calls,
                                    List<ToolCallResult> results) {
        for (int i = 0; i < calls.size(); i++) {
            ToolCall call = calls.get(i);
            ToolCallResult result = i < results.size() ? results.get(i) : new ToolCallResult(call.getName(), false, 1, 0, "工具执行失败");
            ObjectNode toolMessage = context.getObjectMapper().createObjectNode();
            toolMessage.put("role", "tool");
            toolMessage.put("tool_call_id", call.getId());
            toolMessage.put("content", result.getOutput());
            messages.add(toolMessage);
        }
    }

    private ArrayNode toolsFor(List<String> forced) {
        ArrayNode all = registry.getToolDefinitions();
        if (forced == null || forced.isEmpty()) return all;
        Set<String> allowed = new HashSet<>();
        for (String name : forced) allowed.add(registry.canonicalName(name));
        ArrayNode filtered = com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.arrayNode();
        for (JsonNode item : all) if (allowed.contains(item.path("function").path("name").asText())) filtered.add(item);
        return filtered;
    }

    private AgentPlan createPlan(AgentRequest request) {
        AgentPlan plan = new AgentPlan(request.getMessage());
        for (String name : request.getForcedToolNames()) plan.getExpectedTools().add(registry.canonicalName(name));
        String text = request.getMessage().toLowerCase(Locale.ROOT);
        if (text.contains("天气")) plan.getExpectedTools().add("get_weather");
        return plan;
    }

    private boolean mustForceTool(AgentRequest request, Set<String> used) {
        for (String name : request.getForcedToolNames()) if (!used.contains(registry.canonicalName(name))) return true;
        return false;
    }

    private String firstMissingForced(AgentRequest request, Set<String> used) {
        for (String name : request.getForcedToolNames()) {
            String canonical = registry.canonicalName(name);
            if (!used.contains(canonical)) return canonical;
        }
        return null;
    }

    private ToolCall forcedCall(AgentExecutionContext context, String name) {
        if (name == null) return null;
        ObjectNode args = context.getObjectMapper().createObjectNode();
        String prompt = context.getRequest().getMessage();
        switch (name) {
            case "analyze_image", "edit_image" -> {
                args.put("prompt", prompt);
                if (!context.getRequest().getAttachments().isEmpty()) args.put("attachmentId", context.getRequest().getAttachments().get(0).getId());
            }
            case "generate_image" -> args.put("prompt", prompt);
            case "analyze_file" -> args.put("question", prompt);
            case "transcribe_audio" -> args.put("prompt", prompt);
            case "synthesize_speech" -> args.put("text", prompt);
            default -> { return null; }
        }
        return new ToolCall("forced-" + name, name, args);
    }

    private static String stringify(com.sekai.sekai_form.agent.ToolResult<?> result) {
        if (result == null) return "工具无返回结果";
        if (!result.getMessage().isBlank() && result.getData() == null) return result.getMessage();
        if (result.getData() == null) return result.getMessage();
        return result.getData().toString();
    }

    private static String mediaUrl(List<ToolCallResult> results, String key, String current) {
        for (ToolCallResult result : results) {
            String output = result.getOutput();
            if (output.startsWith(key + "=")) return output.substring((key + "=").length());
        }
        return current;
    }

    private static long elapsed(long started) { return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started); }
    private static String safeModelFailure(String message) {
        if (message == null || message.isBlank()) return "当前服务暂时不可用，请稍后再试。";
        if (message.contains("鉴权失败")) return "模型鉴权失败，请检查 API Key。";
        if (message.contains("接口地址无效")) return "模型接口地址无效，请检查 API URL。";
        if (message.contains("无法连接")) return "无法连接模型服务，请检查网络或 API URL。";
        if (message.contains("API URL 必须")) return "API URL 配置无效，请填写 http 或 https 地址。";
        if (message.contains("请求参数无效")) return "模型请求参数无效，请检查模型配置。";
        if (message.contains("频率受限")) return "模型服务频率受限，请稍后重试。";
        return "当前服务暂时不可用，请稍后再试。";
    }
    private static void notify(AgentProgressCallback callback, String phase, String message) { if (callback != null) callback.onProgress(phase, message); }
}
