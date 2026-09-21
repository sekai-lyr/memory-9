package com.sekai.sekai_form.service.impl;

import com.sekai.sekai_form.agent.AgentAttachment;
import com.sekai.sekai_form.agent.AgentRequest;
import com.sekai.sekai_form.agent.AgentResult;
import com.sekai.sekai_form.agent.AgentService;
import com.sekai.sekai_form.dataobject.Live2DChatConfigDO;
import com.sekai.sekai_form.dataobject.Live2DDialogueDO;
import com.sekai.sekai_form.dataobject.Live2DModelDO;
import com.sekai.sekai_form.mapper.Live2DChatConfigMapper;
import com.sekai.sekai_form.mapper.Live2DDialogueMapper;
import com.sekai.sekai_form.mapper.Live2DModelMapper;
import com.sekai.sekai_form.model.ChatResponse;
import com.sekai.sekai_form.model.Result;
import com.sekai.sekai_form.service.AgentProgressService;
import com.sekai.sekai_form.service.Live2DService;
import com.sekai.sekai_form.tool.ConstellationTool;
import com.sekai.sekai_form.tool.CurrentTimeTool;
import com.sekai.sekai_form.tool.JokeTool;
import com.sekai.sekai_form.tool.NewsTool;
import com.sekai.sekai_form.tool.ToolRegistry;
import com.sekai.sekai_form.tool.TranslateTool;
import com.sekai.sekai_form.tool.WeatherTool;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class Live2DServiceImpl implements Live2DService {
    private static final Path MODELS_DIR = Paths.get("models", "2d");
    private final Live2DModelMapper modelMapper;
    private final Live2DDialogueMapper dialogueMapper;
    private final Live2DChatConfigMapper chatConfigMapper;
    private final AgentService agentService;
    private final ToolRegistry toolRegistry;
    private final AgentProgressService progressService;

    public Live2DServiceImpl(Live2DModelMapper modelMapper, Live2DDialogueMapper dialogueMapper,
                             Live2DChatConfigMapper chatConfigMapper, AgentService agentService,
                             ToolRegistry toolRegistry, AgentProgressService progressService) {
        this.modelMapper = modelMapper;
        this.dialogueMapper = dialogueMapper;
        this.chatConfigMapper = chatConfigMapper;
        this.agentService = agentService;
        this.toolRegistry = toolRegistry;
        this.progressService = progressService;
        try { Files.createDirectories(MODELS_DIR); } catch (IOException ignored) { }
        toolRegistry.register(new WeatherTool());
        toolRegistry.register(new JokeTool());
        toolRegistry.register(new CurrentTimeTool());
        toolRegistry.register(new TranslateTool());
        toolRegistry.register(new NewsTool());
        toolRegistry.register(new ConstellationTool());
    }

    @Override public Result<List<Live2DModelDO>> listModels() { return Result.ok(modelMapper.listAll()); }

    @Override public Result<Live2DModelDO> getModel(Long id) {
        Live2DModelDO model = modelMapper.findById(id);
        return model == null ? Result.fail("模型不存在") : Result.ok(model);
    }

    @Override public Result<Live2DModelDO> uploadModel(MultipartFile file, String displayName) {
        try {
            String filename = file.getOriginalFilename();
            if (filename == null || !filename.toLowerCase().endsWith(".zip")) return Result.fail("请上传zip文件");
            String baseName = filename.substring(0, filename.length() - 4).replaceAll("[^\\p{L}\\p{N}_-]", "_");
            Path root = MODELS_DIR.toAbsolutePath().normalize();
            Path targetDir = root.resolve(baseName).normalize();
            if (!targetDir.startsWith(root)) return Result.fail("模型名称无效");
            Files.createDirectories(targetDir);
            try (ZipInputStream zip = new ZipInputStream(file.getInputStream())) {
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    Path output = targetDir.resolve(entry.getName()).normalize();
                    if (!output.startsWith(targetDir)) continue;
                    if (entry.isDirectory()) Files.createDirectories(output);
                    else {
                        if (output.getParent() != null) Files.createDirectories(output.getParent());
                        try (OutputStream stream = Files.newOutputStream(output)) { zip.transferTo(stream); }
                    }
                }
            }
            Live2DModelDO model = new Live2DModelDO();
            model.setModelName(baseName);
            model.setModelPath(targetDir.toString());
            model.setDisplayName(displayName);
            modelMapper.insert(model);
            return Result.ok("上传成功", model);
        } catch (IOException ex) {
            return Result.fail("上传失败");
        }
    }

    @Override public Result<Void> deleteModel(Long id) {
        Live2DModelDO model = modelMapper.findById(id);
        if (model != null && model.getModelPath() != null) {
            try {
                Path target = Paths.get(model.getModelPath()).toAbsolutePath().normalize();
                Path root = MODELS_DIR.toAbsolutePath().normalize();
                if (target.startsWith(root) && Files.exists(target)) Files.walk(target).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            } catch (IOException ignored) { }
        }
        modelMapper.deleteById(id);
        return Result.ok("删除成功", null);
    }

    @Override public Result<List<Live2DDialogueDO>> listDialogues(Long modelId) { return Result.ok(dialogueMapper.listByModelId(modelId)); }

    @Override public Result<Live2DDialogueDO> getRandomDialogue(Long modelId, String category) {
        List<Live2DDialogueDO> list = dialogueMapper.listByCategory(modelId, category);
        return list == null || list.isEmpty() ? Result.fail("无对话") : Result.ok(list.get(new Random().nextInt(list.size())));
    }

    @Override public Result<ChatResponse> chat(Long modelId, String message, List<Map<String, Object>> history) {
        return chat(modelId, message, history, UUID.randomUUID().toString(), "anonymous");
    }

    @Override public Result<ChatResponse> chat(Long modelId, String message, List<Map<String, Object>> history,
                                               String conversationId, String userId) {
        AgentRequest request = AgentRequest.builder().modelId(modelId).message(message).modality("TEXT")
                .conversationId(conversationId).userId(userId).externalHistory(history).build();
        return toChatResponse(runAgent(request));
    }

    @Override public Result<ChatResponse> recognizeImage(Long modelId, MultipartFile image, String question) {
        return recognizeImage(modelId, image, question, UUID.randomUUID().toString(), "anonymous");
    }

    @Override public Result<ChatResponse> recognizeImage(Long modelId, MultipartFile image, String question,
                                                         String conversationId, String userId) {
        return runAttachment(modelId, image,
                question == null || question.isBlank() ? "请描述这张图片，并识别其中的 Live2D 角色、场景和文字" : question,
                "IMAGE", List.of("analyze_image"), conversationId, userId);
    }

    @Override public Result<ChatResponse> generateImage(Long modelId, String prompt) {
        return generateImage(modelId, prompt, UUID.randomUUID().toString(), "anonymous");
    }

    @Override public Result<ChatResponse> generateImage(Long modelId, String prompt,
                                                        String conversationId, String userId) {
        String value = prompt == null || prompt.isBlank() ? "生成一张 Live2D 角色插画" : prompt;
        AgentRequest request = AgentRequest.builder().modelId(modelId).message(value).modality("IMAGE")
                .conversationId(conversationId).userId(userId).forcedToolNames(List.of("generate_image")).build();
        return toChatResponse(runAgent(request));
    }

    @Override public Result<Live2DChatConfigDO> getChatConfig(Long modelId) {
        Live2DChatConfigDO config = chatConfigMapper.getByModelId(modelId);
        if (config == null) return Result.fail("聊天配置不存在");
        config.setApiKey(config.getApiKey() == null || config.getApiKey().isBlank() ? "" : "********");
        return Result.ok(config);
    }

    @Override public Result<Void> saveChatConfig(Live2DChatConfigDO config) {
        Live2DChatConfigDO existing = chatConfigMapper.getByModelId(config.getModelId());
        if (existing == null) chatConfigMapper.insert(config);
        else {
            if (config.getId() == null) config.setId(existing.getId());
            if (config.getApiKey() == null || config.getApiKey().isBlank() || config.getApiKey().equals("********")) config.setApiKey(existing.getApiKey());
            chatConfigMapper.update(config);
        }
        return Result.ok("保存成功", null);
    }

    @Override public Result<ChatResponse> analyzeFile(Long modelId, MultipartFile file, String question,
                                                      String conversationId, String userId) {
        return runAttachment(modelId, file, question == null || question.isBlank() ? "请总结这个文件" : question,
                "FILE", List.of("analyze_file"), conversationId, userId);
    }

    @Override public Result<ChatResponse> processAudio(Long modelId, MultipartFile audio, String question,
                                                       String conversationId, String userId) {
        return runAttachment(modelId, audio, question == null ? "" : question,
                "AUDIO", List.of("transcribe_audio"), conversationId, userId);
    }

    @Override public Result<ChatResponse> editImage(Long modelId, MultipartFile image, String instruction,
                                                    String conversationId, String userId) {
        return runAttachment(modelId, image,
                instruction == null || instruction.isBlank() ? "请优化这张图片的画面表现" : instruction,
                "IMAGE", List.of("edit_image"), conversationId, userId);
    }

    private Result<ChatResponse> runAttachment(Long modelId, MultipartFile file, String message, String modality,
                                               List<String> forcedTools, String conversationId, String userId) {
        try {
            if (file == null || file.isEmpty()) return Result.fail("附件不能为空");
            byte[] bytes = file.getBytes();
            if (bytes.length > 16 * 1024 * 1024) return Result.fail("附件过大");
            AgentAttachment attachment = new AgentAttachment(UUID.randomUUID().toString(),
                    file.getOriginalFilename() == null ? "attachment" : file.getOriginalFilename(),
                    file.getContentType(), modality, bytes);
            AgentRequest request = AgentRequest.builder().modelId(modelId).message(message).modality(modality)
                    .conversationId(conversationId).userId(userId).addAttachment(attachment).forcedToolNames(forcedTools).build();
            return toChatResponse(runAgent(request));
        } catch (IOException ex) {
            return Result.fail("附件读取失败");
        }
    }

    private Result<ChatResponse> toChatResponse(AgentResult result) {
        if (result == null || !result.isSuccess()) return Result.fail(result == null ? "处理失败" : result.getReply());
        ChatResponse response = new ChatResponse(result.getReply(), result.getMotion());
        response.setImageUrl(result.getImageUrl());
        response.setAudioUrl(result.getAudioUrl());
        return Result.ok(response);
    }

    private AgentResult runAgent(AgentRequest request) {
        progressService.publish(request.getConversationId(), "accepted", "请求已进入 Agent 主循环");
        AgentResult result = agentService.run(request, (phase, message) ->
                progressService.publish(request.getConversationId(), phase, message));
        progressService.publish(request.getConversationId(), result.isSuccess() ? "completed" : "failed",
                result.isSuccess() ? "处理完成" : result.getReply());
        return result;
    }
}
