package com.sekai.sekai_form.control;

import com.sekai.sekai_form.dataobject.Live2DChatConfigDO;
import com.sekai.sekai_form.dataobject.Live2DModelDO;
import com.sekai.sekai_form.model.ChatResponse;
import com.sekai.sekai_form.model.Result;
import com.sekai.sekai_form.service.Live2DService;
import com.sekai.sekai_form.service.MediaArtifactService;
import com.sekai.sekai_form.service.AgentProgressService;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/live2d")
public class Live2DController {
    private final Live2DService live2DService;
    private final MediaArtifactService mediaArtifactService;
    private final AgentProgressService progressService;
    public Live2DController(Live2DService live2DService, MediaArtifactService mediaArtifactService,
                            AgentProgressService progressService) {
        this.live2DService = live2DService;
        this.mediaArtifactService = mediaArtifactService;
        this.progressService = progressService;
    }

    @GetMapping("/models") public Result<List<Live2DModelDO>> listModels() { return live2DService.listModels(); }

    @GetMapping("/models/{id}") public Result<Live2DModelDO> getModel(@PathVariable Long id) { return live2DService.getModel(id); }

    @PostMapping("/upload") public Result<Live2DModelDO> upload(@RequestParam("file") MultipartFile file, @RequestParam(required = false) String displayName) {
        return live2DService.uploadModel(file, displayName);
    }

    @DeleteMapping("/models/{id}") public Result<Void> delete(@PathVariable Long id) { return live2DService.deleteModel(id); }

    @GetMapping("/dialogues/{modelId}") public Result<?> dialogues(@PathVariable Long modelId) { return live2DService.listDialogues(modelId); }

    @PostMapping("/chat/{modelId}")
    public Result<ChatResponse> chat(@PathVariable Long modelId, @RequestBody Map<String, Object> body, HttpSession session) {
        String message = (String) body.get("message");
        if (message == null || message.isBlank()) return Result.fail("消息不能为空");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> history = (List<Map<String, Object>>) body.get("history");
        return live2DService.chat(modelId, message, history, conversationId(body, modelId, session), "session-user");
    }

    @PostMapping("/chat/{modelId}/recognize")
    public Result<ChatResponse> recognizeImage(@PathVariable Long modelId,
                                                @RequestParam("image") MultipartFile image,
                                                @RequestParam(required = false, defaultValue = "") String question,
                                                @RequestParam(required = false, defaultValue = "") String conversationId,
                                                HttpSession session) {
        return live2DService.recognizeImage(modelId, image, question,
                conversationId(conversationId, modelId, session), "session-user");
    }

    @PostMapping("/chat/{modelId}/generate")
    public Result<ChatResponse> generateImage(@PathVariable Long modelId,
                                               @RequestBody Map<String, Object> body,
                                               HttpSession session) {
        String prompt = body == null || body.get("prompt") == null ? "" : body.get("prompt").toString();
        return live2DService.generateImage(modelId, prompt,
                conversationId(body, modelId, session), "session-user");
    }

    @GetMapping("/chat/config/{modelId}")
    public Result<Live2DChatConfigDO> getChatConfig(@PathVariable Long modelId) {
        return live2DService.getChatConfig(modelId);
    }

    @PostMapping("/chat/config")
    public Result<Void> saveChatConfig(@RequestBody Live2DChatConfigDO config) {
        return live2DService.saveChatConfig(config);
    }

    @PostMapping("/chat/{modelId}/file")
    public Result<ChatResponse> analyzeFile(@PathVariable Long modelId, @RequestParam("file") MultipartFile file,
                                             @RequestParam(required = false, defaultValue = "") String question,
                                             @RequestParam(required = false, defaultValue = "") String conversationId,
                                             HttpSession session) {
        return live2DService.analyzeFile(modelId, file, question, conversationId(conversationId, modelId, session), "session-user");
    }

    @PostMapping("/chat/{modelId}/audio")
    public Result<ChatResponse> processAudio(@PathVariable Long modelId, @RequestParam("audio") MultipartFile audio,
                                              @RequestParam(required = false, defaultValue = "") String question,
                                              @RequestParam(required = false, defaultValue = "") String conversationId,
                                              HttpSession session) {
        return live2DService.processAudio(modelId, audio, question, conversationId(conversationId, modelId, session), "session-user");
    }

    @PostMapping("/chat/{modelId}/edit")
    public Result<ChatResponse> editImage(@PathVariable Long modelId,
                                          @RequestParam("image") MultipartFile image,
                                          @RequestParam(required = false, defaultValue = "") String instruction,
                                          @RequestParam(required = false, defaultValue = "") String conversationId,
                                          HttpSession session) {
        return live2DService.editImage(modelId, image, instruction,
                conversationId(conversationId, modelId, session), "session-user");
    }

    @GetMapping("/agent-media/{id}")
    public ResponseEntity<InputStreamResource> media(@PathVariable String id) {
        Path path = mediaArtifactService.resolve(id);
        if (path == null) return ResponseEntity.notFound().build();
        try {
            return ResponseEntity.ok().contentType(MediaType.parseMediaType("audio/wav"))
                    .contentLength(Files.size(path)).body(new InputStreamResource(Files.newInputStream(path)));
        } catch (Exception ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/chat/{modelId}/progress")
    public Result<AgentProgressService.Snapshot> progress(@PathVariable Long modelId,
                                                            @RequestParam(required = false, defaultValue = "") String conversationId,
                                                            HttpSession session) {
        AgentProgressService.Snapshot snapshot = progressService.get(conversationId(conversationId, modelId, session));
        if (snapshot == null) snapshot = new AgentProgressService.Snapshot("idle", "暂无进行中的任务", java.time.Instant.now());
        return Result.ok(snapshot);
    }

    private String conversationId(Map<String, ?> body, Long modelId, HttpSession session) {
        Object value = body == null ? null : body.get("conversationId");
        return conversationId(value == null ? "" : value.toString(), modelId, session);
    }

    private String conversationId(String value, Long modelId, HttpSession session) {
        if (value != null && !value.isBlank()) return value.length() > 200 ? value.substring(0, 200) : value;
        String sessionId = session == null ? "request" : session.getId();
        return "live2d:" + modelId + ":" + sessionId;
    }
}
