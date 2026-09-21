package com.sekai.sekai_form.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sekai.sekai_form.dataobject.Live2DChatConfigDO;
import com.sekai.sekai_form.security.AgentSecurityGuard;
import com.sekai.sekai_form.service.AgentSessionStateService;
import com.sekai.sekai_form.service.MediaArtifactService;
import com.sekai.sekai_form.service.ImageGenerationGateway;
import com.sekai.sekai_form.tool.AudioTranscriptionTool;
import com.sekai.sekai_form.tool.FileAnalysisTool;
import com.sekai.sekai_form.tool.ImageAnalysisTool;
import com.sekai.sekai_form.tool.ImageEditTool;
import com.sekai.sekai_form.tool.ImageGenerationTool;
import com.sekai.sekai_form.tool.SpeechSynthesisTool;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.*;

class SecurityAndMediaTest {
    @Test void securityGuardRemovesPathsSecretsAndInternalIds() {
        AgentSecurityGuard guard = new AgentSecurityGuard();
        String cleaned = guard.clean("C:\\private\\token.txt Bearer abc123 conversation_id=secret-1");
        assertFalse(cleaned.contains("C:\\private"));
        assertFalse(cleaned.contains("Bearer abc123"));
        assertFalse(cleaned.contains("secret-1"));
    }

    @Test void speechToolReturnsPublicArtifactReferenceWithoutFilesystemPath() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        MediaArtifactService artifacts = new MediaArtifactService();
        SpeechSynthesisTool tool = new SpeechSynthesisTool(artifacts);
        ObjectNode args = mapper.createObjectNode();
        args.put("text", "你好，Haru");
        AgentRequest request = AgentRequest.builder().modelId(1L).conversationId("media-test").message("朗读").build();
        AgentExecutionContext context = new AgentExecutionContext(request, new Live2DChatConfigDO(), mapper, new AgentSessionStateService());
        ToolResult<?> result = tool.execute(args, context);
        assertTrue(result.isSuccess());
        assertTrue(result.getData().toString().startsWith("audioUrl=/api/live2d/agent-media/"));
        assertFalse(result.getData().toString().contains("\\") && result.getData().toString().contains(":"));
    }

    @Test void fileAndAudioInputsAreProcessedByDedicatedTools() {
        ObjectMapper mapper = new ObjectMapper();
        AgentRequest request = AgentRequest.builder().modelId(1L).conversationId("multimodal-test").message("分析附件")
                .addAttachment(new AgentAttachment("file-1", "notes.txt", "text/plain", "FILE", "Live2D 角色喜欢草莓牛奶".getBytes()))
                .addAttachment(new AgentAttachment("audio-1", "voice.txt", "audio/wav", "AUDIO", "请提醒我休息".getBytes())).build();
        AgentExecutionContext context = new AgentExecutionContext(request, new Live2DChatConfigDO(), mapper, new AgentSessionStateService());
        ToolResult<?> file = new FileAnalysisTool().execute(mapper.createObjectNode().put("question", "总结"), context);
        ToolResult<?> audio = new AudioTranscriptionTool().execute(mapper.createObjectNode(), context);
        assertTrue(file.isSuccess());
        assertTrue(file.getData().toString().contains("草莓牛奶"));
        assertTrue(audio.isSuccess());
        assertEquals("请提醒我休息", audio.getData());
    }

    @Test void imageAnalyzeGenerateAndEditUseMultimodalTools() {
        ObjectMapper mapper = new ObjectMapper();
        AgentAttachment image = new AgentAttachment("image-1", "character.png", "image/png", "IMAGE",
                new byte[]{1, 2, 3});
        AgentRequest request = AgentRequest.builder().modelId(1L).conversationId("image-test")
                .message("处理图片").addAttachment(image).build();
        Live2DChatConfigDO config = new Live2DChatConfigDO();
        config.setApiKey("test-key");
        AgentExecutionContext context = new AgentExecutionContext(request, config, mapper, new AgentSessionStateService());
        AgentModelClient vision = (cfg, messages, tools) -> {
            ObjectNode result = mapper.createObjectNode();
            ObjectNode choice = mapper.createObjectNode();
            ObjectNode responseMessage = mapper.createObjectNode();
            responseMessage.put("content", "这是一个 Live2D 角色");
            choice.set("message", responseMessage);
            result.set("choices", mapper.createArrayNode().add(choice));
            return result;
        };
        ToolResult<?> analyzed = new ImageAnalysisTool(vision).execute(
                mapper.createObjectNode().put("prompt", "描述图片"), context);
        ImageGenerationGateway gateway = new ImageGenerationGateway() {
            @Override public String generate(Live2DChatConfigDO cfg, String prompt) { return "https://media.example/generated.png"; }
            @Override public String edit(Live2DChatConfigDO cfg, String prompt, AgentAttachment source) { return "https://media.example/edited.png"; }
        };
        ToolResult<?> generated = new ImageGenerationTool(gateway).execute(
                mapper.createObjectNode().put("prompt", "生成角色"), context);
        ToolResult<?> edited = new ImageEditTool(gateway).execute(
                mapper.createObjectNode().put("prompt", "调整配色"), context);
        assertTrue(analyzed.isSuccess());
        assertTrue(analyzed.getData().toString().contains("Live2D"));
        assertEquals("imageUrl=https://media.example/generated.png", generated.getData());
        assertEquals("imageUrl=https://media.example/edited.png", edited.getData());
    }
}
