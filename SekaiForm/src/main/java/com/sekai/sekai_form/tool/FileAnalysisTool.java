package com.sekai.sekai_form.tool;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sekai.sekai_form.agent.AgentAttachment;
import com.sekai.sekai_form.agent.AgentExecutionContext;
import com.sekai.sekai_form.agent.ToolResult;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
public class FileAnalysisTool implements AgentTool {
    @Override public String name() { return "analyze_file"; }
    @Override public String description() { return "读取并分析用户上传的 TXT、CSV、JSON、PDF 或 Office 文件内容。"; }
    @Override public ObjectNode getParametersSchema() { return AgentToolSupport.schema(new String[][]{{"question", "string", "对文件的分析要求"}, {"attachmentId", "string", "文件附件引用"}}, "question"); }
    @Override public ToolResult<?> execute(ObjectNode args, AgentExecutionContext context) {
        AgentAttachment file = findFile(args.path("attachmentId").asText(""), context);
        if (file == null) return ToolResult.failure("没有可分析的文件", false);
        try {
            String text = extract(file);
            if (text.isBlank()) return ToolResult.failure("文件中没有可读取的文本内容", false);
            String question = args.path("question").asText("请总结文件内容");
            return ToolResult.success("文件已读取", "文件名：" + safeName(file.getName()) + "\n用户要求：" + question + "\n内容摘录：\n" + limit(text, 9000));
        } catch (Exception ex) {
            return ToolResult.failure("文件分析失败", false);
        }
    }

    private AgentAttachment findFile(String id, AgentExecutionContext context) {
        for (AgentAttachment item : context.getRequest().getAttachments()) {
            if (!item.isAudio() && !item.isImage() && (id.isBlank() || id.equals(item.getId()))) return item;
        }
        AgentAttachment item = context.getSessionStateService().getLastAttachment(context.getRequest().getConversationId());
        return item != null && !item.isAudio() && !item.isImage() ? item : null;
    }

    private String extract(AgentAttachment file) throws Exception {
        String name = safeName(file.getName()).toLowerCase();
        byte[] bytes = file.getContent();
        if (name.endsWith(".docx") || name.endsWith(".xlsx") || name.endsWith(".pptx")) return zipText(bytes);
        if (name.endsWith(".pdf")) return new String(bytes, StandardCharsets.ISO_8859_1).replaceAll("[^\\p{Print}\\r\\n\\t]", " ");
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private String zipText(byte[] bytes) throws Exception {
        StringBuilder result = new StringBuilder();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().endsWith(".xml")) result.append(new String(zip.readAllBytes(), StandardCharsets.UTF_8).replaceAll("<[^>]+>", " ")).append('\n');
            }
        }
        return result.toString();
    }

    private static String safeName(String name) { return name == null ? "文件" : name.replaceAll("[\\\\/:*?\"<>|]", "_"); }
    private static String limit(String value, int max) { return value.length() <= max ? value : value.substring(0, max) + "…"; }
}
