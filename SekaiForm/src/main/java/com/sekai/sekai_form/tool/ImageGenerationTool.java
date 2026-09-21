package com.sekai.sekai_form.tool;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sekai.sekai_form.agent.AgentExecutionContext;
import com.sekai.sekai_form.agent.ToolResult;
import com.sekai.sekai_form.service.ImageGenerationGateway;
import org.springframework.stereotype.Component;

@Component
public class ImageGenerationTool implements AgentTool {
    private final ImageGenerationGateway imageService;
    public ImageGenerationTool(ImageGenerationGateway imageService) { this.imageService = imageService; }
    @Override public String name() { return "generate_image"; }
    @Override public String description() { return "根据文字生成 Live2D 角色或动漫风格图片。"; }
    @Override public ObjectNode getParametersSchema() { return AgentToolSupport.schema(new String[][]{{"prompt", "string", "图片描述"}}, "prompt"); }
    @Override public ToolResult<?> execute(ObjectNode args, AgentExecutionContext context) {
        try { return ToolResult.success("图片已生成", "imageUrl=" + imageService.generate(context.getConfig(), args.path("prompt").asText(context.getRequest().getMessage()))); }
        catch (Exception ex) { return ToolResult.failure("图片生成服务暂时不可用", true); }
    }
}
