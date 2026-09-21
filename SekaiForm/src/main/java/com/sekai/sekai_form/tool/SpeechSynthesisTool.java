package com.sekai.sekai_form.tool;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sekai.sekai_form.agent.AgentExecutionContext;
import com.sekai.sekai_form.agent.ToolResult;
import com.sekai.sekai_form.service.MediaArtifactService;
import org.springframework.stereotype.Component;

@Component
public class SpeechSynthesisTool implements AgentTool {
    private final MediaArtifactService artifactService;
    public SpeechSynthesisTool(MediaArtifactService artifactService) { this.artifactService = artifactService; }
    @Override public String name() { return "synthesize_speech"; }
    @Override public String description() { return "将最终文本生成可播放的语音输出附件。"; }
    @Override public ObjectNode getParametersSchema() { return AgentToolSupport.schema(new String[][]{{"text", "string", "需要输出为语音的文本"}}, "text"); }
    @Override public ToolResult<?> execute(ObjectNode args, AgentExecutionContext context) {
        try {
            String url = artifactService.createAudioUrl(args.path("text").asText(context.getRequest().getMessage()));
            return ToolResult.success("语音输出已生成", "audioUrl=" + url);
        } catch (Exception ex) {
            return ToolResult.failure("语音输出暂时不可用", true);
        }
    }
}
