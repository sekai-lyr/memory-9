package com.sekai.sekai_form.tool;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sekai.sekai_form.agent.AgentAttachment;
import com.sekai.sekai_form.agent.AgentExecutionContext;
import com.sekai.sekai_form.agent.ToolResult;
import com.sekai.sekai_form.service.ImageGenerationGateway;
import org.springframework.stereotype.Component;

@Component
public class ImageEditTool implements AgentTool {
    private final ImageGenerationGateway imageService;
    public ImageEditTool(ImageGenerationGateway imageService) { this.imageService = imageService; }
    @Override public String name() { return "edit_image"; }
    @Override public String description() { return "基于当前图片执行修改、风格转换、增删元素等编辑。"; }
    @Override public ObjectNode getParametersSchema() { return AgentToolSupport.schema(new String[][]{{"prompt", "string", "编辑指令"}, {"attachmentId", "string", "图片附件引用"}}, "prompt"); }
    @Override public ToolResult<?> execute(ObjectNode args, AgentExecutionContext context) {
        AgentAttachment source = ImageAnalysisTool.findAttachment(args.path("attachmentId").asText(""), context);
        if (source == null || !source.isImage()) return ToolResult.failure("没有可编辑的图片", false);
        try { return ToolResult.success("图片已编辑", "imageUrl=" + imageService.edit(context.getConfig(), args.path("prompt").asText(context.getRequest().getMessage()), source)); }
        catch (Exception ex) { return ToolResult.failure("图片编辑服务暂时不可用", true); }
    }
}
