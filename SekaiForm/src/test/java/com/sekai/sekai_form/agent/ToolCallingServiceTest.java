package com.sekai.sekai_form.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sekai.sekai_form.dataobject.Live2DChatConfigDO;
import com.sekai.sekai_form.security.AgentSecurityGuard;
import com.sekai.sekai_form.service.AgentSessionStateService;
import com.sekai.sekai_form.tool.AgentTool;
import com.sekai.sekai_form.tool.ToolRegistry;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ToolCallingServiceTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test void ordinaryTextReturnsWithoutToolCall() {
        FakeModel model = new FakeModel(reply("你好，主人。"));
        ToolRegistry registry = new ToolRegistry();
        ToolCallingService service = new ToolCallingService(model, registry, new AgentSecurityGuard());
        AgentLoopResult result = service.run(context(registry), messages("hello"), null);
        assertEquals("你好，主人。", result.reply());
        assertTrue(result.toolCalls().isEmpty());
    }

    @Test void singleToolCallIsWrittenBackBeforeFinalAnswer() {
        AtomicInteger executed = new AtomicInteger();
        ToolRegistry registry = new ToolRegistry().register(new StubTool("lookup", executed, false, null));
        FakeModel model = new FakeModel(toolReply("lookup", "city", "杭州"), reply("杭州结果已收到。"));
        AgentLoopResult result = new ToolCallingService(model, registry, new AgentSecurityGuard()).run(context(registry), messages("查一下"), null);
        assertEquals("杭州结果已收到。", result.reply());
        assertEquals(1, executed.get());
        assertEquals(2, model.calls);
    }

    @Test void acceptsMemory14CamelCaseToolNameAlias() {
        AtomicInteger executed = new AtomicInteger();
        ToolRegistry registry = new ToolRegistry().register(new StubTool("get_weather", executed, false, null));
        FakeModel model = new FakeModel(toolReply("getWeather", "city", "杭州"), reply("天气结果已收到。"));
        AgentLoopResult result = new ToolCallingService(model, registry, new AgentSecurityGuard())
                .run(context(registry), messages("查询天气"), null);
        assertEquals("天气结果已收到。", result.reply());
        assertEquals(1, executed.get());
    }

    @Test void dependentToolsRunInModelOrder() {
        List<String> order = new ArrayList<>();
        ToolRegistry registry = new ToolRegistry()
                .register(new StubTool("weather", null, false, order))
                .register(new StubTool("summary", null, false, order));
        FakeModel model = new FakeModel(toolReply("weather", "city", "杭州"), toolReply("summary", "prompt", "使用天气结果整理"), reply("已完成。"));
        AgentLoopResult result = new ToolCallingService(model, registry, new AgentSecurityGuard()).run(context(registry), messages("查天气并整理结果"), null);
        assertEquals("已完成。", result.reply());
        assertEquals(List.of("weather", "summary"), order);
    }

    @Test void independentToolsExecuteConcurrentlyAndKeepWritebackOrder() throws Exception {
        CountDownLatch started = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        ToolRegistry registry = new ToolRegistry()
                .register(new StubTool("one", null, false, null, started, release))
                .register(new StubTool("two", null, false, null, started, release));
        FakeModel model = new FakeModel(toolReply("one", "value", "1", "two", "value", "2"), reply("并行完成。"));
        Thread runner = new Thread(() -> {
            new ToolCallingService(model, registry, new AgentSecurityGuard()).run(context(registry), messages("执行两个独立步骤"), null);
        });
        runner.start();
        assertTrue(started.await(3, TimeUnit.SECONDS));
        release.countDown();
        runner.join(3000);
        assertFalse(runner.isAlive());
    }

    @Test void retryableToolFailureRetriesOnce() {
        AtomicInteger attempts = new AtomicInteger();
        ToolRegistry registry = new ToolRegistry().register(new StubTool("flaky", attempts, true, null));
        FakeModel model = new FakeModel(toolReply("flaky", "value", "ok"), reply("重试后完成。"));
        AgentLoopResult result = new ToolCallingService(model, registry, new AgentSecurityGuard()).run(context(registry), messages("执行不稳定工具"), null);
        assertEquals("重试后完成。", result.reply());
        assertEquals(2, attempts.get());
        assertTrue(result.toolCalls().get(0).isSuccess());
        assertEquals(2, result.toolCalls().get(0).getAttempt());
    }

    @Test void modelFailureKeepsSafeDiagnosticAfterOneRetry() {
        AgentModelClient failing = (config, messages, tools) -> {
            throw new IllegalStateException("模型鉴权失败，请检查 API Key。");
        };
        AgentLoopResult result = new ToolCallingService(failing, new ToolRegistry(), new AgentSecurityGuard())
                .run(context(new ToolRegistry()), messages("测试模型错误"), null);
        assertEquals("模型鉴权失败，请检查 API Key。", result.reply());
        assertFalse(result.success());
    }

    private AgentExecutionContext context(ToolRegistry registry) {
        Live2DChatConfigDO config = new Live2DChatConfigDO();
        config.setModelName("test");
        AgentRequest request = AgentRequest.builder().modelId(1L).conversationId("test-conversation").message("测试").build();
        return new AgentExecutionContext(request, config, mapper, new AgentSessionStateService());
    }

    private ArrayNode messages(String value) {
        ArrayNode messages = mapper.createArrayNode();
        ObjectNode user = mapper.createObjectNode();
        user.put("role", "user");
        user.put("content", value);
        messages.add(user);
        return messages;
    }

    private JsonNode reply(String content) {
        ObjectNode message = mapper.createObjectNode();
        message.put("role", "assistant");
        message.put("content", content);
        return response(message);
    }

    private JsonNode toolReply(String name, String key, String value, String... extra) {
        ObjectNode message = mapper.createObjectNode();
        message.put("role", "assistant");
        message.putNull("content");
        ArrayNode calls = mapper.createArrayNode();
        calls.add(toolCall("call-1", name, key, value));
        for (int i = 0; i < extra.length; i += 3) calls.add(toolCall("call-" + (i + 2), extra[i], extra[i + 1], extra[i + 2]));
        message.set("tool_calls", calls);
        return response(message);
    }

    private ObjectNode toolCall(String id, String name, String key, String value) {
        ObjectNode call = mapper.createObjectNode();
        call.put("id", id);
        ObjectNode function = mapper.createObjectNode();
        function.put("name", name);
        ObjectNode args = mapper.createObjectNode();
        args.put(key, value);
        function.put("arguments", args.toString());
        call.set("function", function);
        return call;
    }

    private JsonNode response(ObjectNode message) {
        ObjectNode choice = mapper.createObjectNode();
        choice.set("message", message);
        ArrayNode choices = mapper.createArrayNode();
        choices.add(choice);
        ObjectNode response = mapper.createObjectNode();
        response.set("choices", choices);
        return response;
    }

    private final class FakeModel implements AgentModelClient {
        private final Queue<JsonNode> responses = new ArrayDeque<>();
        private int calls;
        private FakeModel(JsonNode... values) { for (JsonNode value : values) responses.add(value); }
        @Override public JsonNode complete(Live2DChatConfigDO config, ArrayNode messages, ArrayNode tools) {
            calls++;
            return responses.isEmpty() ? reply("") : responses.remove();
        }
    }

    private static final class StubTool implements AgentTool {
        private final String name;
        private final AtomicInteger counter;
        private final boolean flaky;
        private final List<String> order;
        private final CountDownLatch started;
        private final CountDownLatch release;
        private StubTool(String name, AtomicInteger counter, boolean flaky, List<String> order) { this(name, counter, flaky, order, null, null); }
        private StubTool(String name, AtomicInteger counter, boolean flaky, List<String> order, CountDownLatch started, CountDownLatch release) {
            this.name = name; this.counter = counter; this.flaky = flaky; this.order = order; this.started = started; this.release = release;
        }
        @Override public String name() { return name; }
        @Override public String description() { return "test tool"; }
        @Override public ObjectNode getParametersSchema() {
            ObjectMapper testMapper = new ObjectMapper();
            ObjectNode schema = testMapper.createObjectNode(); schema.put("type", "object");
            ObjectNode properties = testMapper.createObjectNode(); ObjectNode value = testMapper.createObjectNode(); value.put("type", "string"); properties.set("value", value);
            ObjectNode city = testMapper.createObjectNode(); city.put("type", "string"); properties.set("city", city);
            ObjectNode prompt = testMapper.createObjectNode(); prompt.put("type", "string"); properties.set("prompt", prompt);
            schema.set("properties", properties); return schema;
        }
        @Override public ToolResult<?> execute(ObjectNode arguments, AgentExecutionContext context) {
            if (counter != null && counter.incrementAndGet() == 1 && flaky) return ToolResult.failure("temporary", true);
            if (order != null) synchronized (order) { order.add(name); }
            if (started != null) { started.countDown(); try { release.await(2, TimeUnit.SECONDS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }
            return ToolResult.success(name + " result");
        }
    }
}
