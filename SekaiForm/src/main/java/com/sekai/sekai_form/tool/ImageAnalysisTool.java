package com.sekai.sekai_form.tool;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sekai.sekai_form.agent.AgentAttachment;
import com.sekai.sekai_form.agent.AgentExecutionContext;
import com.sekai.sekai_form.agent.AgentModelClient;
import com.sekai.sekai_form.agent.ToolResult;
import org.springframework.stereotype.Component;

import java.util.Base64;

@Component
public class ImageAnalysisTool implements AgentTool {
    private final AgentModelClient modelClient;
    public ImageAnalysisTool(AgentModelClient modelClient) { this.modelClient = modelClient; }
    @Override public String name() { return "analyze_image"; }
    @Override public String description() { return "分析当前图片，识别角色、场景、文字和用户指定的视觉细节。"; }
    @Override public ObjectNode getParametersSchema() { return AgentToolSupport.schema(new String[][]{{"prompt", "string", "用户对图片的要求"}, {"attachmentId", "string", "图片附件引用"}}, "prompt"); }
    @Override public ToolResult<?> execute(ObjectNode args, AgentExecutionContext context) {
        AgentAttachment attachment = findAttachment(args.path("attachmentId").asText(""), context);
        if (attachment == null || !attachment.isImage()) return ToolResult.failure("没有可分析的图片", false);
        try {
            ObjectNode user = context.getObjectMapper().createObjectNode();
            user.put("role", "user");
            ArrayNode content = context.getObjectMapper().createArrayNode();
            ObjectNode text = context.getObjectMapper().createObjectNode(); text.put("type", "text"); text.put("text", args.path("prompt").asText("请描述这张图片")); content.add(text);
            ObjectNode image = context.getObjectMapper().createObjectNode(); image.put("type", "image_url");
            ObjectNode imageUrl = context.getObjectMapper().createObjectNode(); imageUrl.put("url", "data:" + attachment.getMediaType() + ";base64," + Base64.getEncoder().encodeToString(attachment.getContent())); image.set("image_url", imageUrl); content.add(image);
            user.set("content", content);
            ArrayNode messages = context.getObjectMapper().createArrayNode(); messages.add(user);
            var response = modelClient.complete(context.getConfig(), messages, context.getObjectMapper().createArrayNode());
            String value = response.path("choices").path(0).path("message").path("content").asText("");
            if (value.isBlank()) value = response.path("output").path("choices").path(0).path("message").path("content").asText("");
            return value.isBlank() ? ToolResult.failure("图片分析没有返回内容", true) : ToolResult.success(value.trim());
        } catch (Exception ex) { return ToolResult.failure("图片分析服务暂时不可用", true); }
    }
    static AgentAttachment findAttachment(String id, AgentExecutionContext context) {
        for (AgentAttachment item : context.getRequest().getAttachments()) if (id.isBlank() || id.equals(item.getId())) return item;
        return context.getSessionStateService().getLastAttachment(context.getRequest().getConversationId());
    }
}
