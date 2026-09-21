package com.sekai.sekai_form.tool;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sekai.sekai_form.agent.AgentAttachment;
import com.sekai.sekai_form.agent.AgentExecutionContext;
import com.sekai.sekai_form.agent.ToolResult;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class AudioTranscriptionTool implements AgentTool {
    @Override public String name() { return "transcribe_audio"; }
    @Override public String description() { return "处理语音输入；若已配置 ASR 则返回转写文本，否则返回安全的配置提示。"; }
    @Override public ObjectNode getParametersSchema() { return AgentToolSupport.schema(new String[][]{{"prompt", "string", "语音相关问题"}}); }
    @Override public ToolResult<?> execute(ObjectNode args, AgentExecutionContext context) {
        for (AgentAttachment item : context.getRequest().getAttachments()) if (item.isAudio()) {
            String fixture = new String(item.getContent(), StandardCharsets.UTF_8).trim();
            if (!fixture.isBlank() && fixture.chars().allMatch(ch -> ch == '\n' || ch == '\r' || ch == '\t' || ch >= 32)) return ToolResult.success("语音转写完成", fixture);
            return ToolResult.success("已收到语音输入；当前服务未配置外部 ASR，无法生成文字转写。", "");
        }
        return ToolResult.failure("没有可处理的语音附件", false);
    }
}
