package com.sekai.sekai_form.control;

import com.sekai.sekai_form.dataobject.Live2DChatConfigDO;
import com.sekai.sekai_form.dataobject.Live2DModelDO;
import com.sekai.sekai_form.model.ChatResponse;
import com.sekai.sekai_form.model.Result;
import com.sekai.sekai_form.service.Live2DService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/live2d")
public class Live2DController {
    private final Live2DService live2DService;
    public Live2DController(Live2DService live2DService) { this.live2DService = live2DService; }

    @GetMapping("/models") public Result<List<Live2DModelDO>> listModels() { return live2DService.listModels(); }

    @GetMapping("/models/{id}") public Result<Live2DModelDO> getModel(@PathVariable Long id) { return live2DService.getModel(id); }

    @PostMapping("/upload") public Result<Live2DModelDO> upload(@RequestParam("file") MultipartFile file, @RequestParam(required = false) String displayName) {
        return live2DService.uploadModel(file, displayName);
    }

    @DeleteMapping("/models/{id}") public Result<Void> delete(@PathVariable Long id) { return live2DService.deleteModel(id); }

    @GetMapping("/dialogues/{modelId}") public Result<?> dialogues(@PathVariable Long modelId) { return live2DService.listDialogues(modelId); }

    @PostMapping("/chat/{modelId}")
    public Result<ChatResponse> chat(@PathVariable Long modelId, @RequestBody Map<String, Object> body) {
        String message = (String) body.get("message");
        if (message == null || message.isBlank()) return Result.fail("消息不能为空");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> history = (List<Map<String, Object>>) body.get("history");
        return live2DService.chat(modelId, message, history);
    }

    @GetMapping("/chat/config/{modelId}")
    public Result<Live2DChatConfigDO> getChatConfig(@PathVariable Long modelId) {
        return live2DService.getChatConfig(modelId);
    }

    @PostMapping("/chat/config")
    public Result<Void> saveChatConfig(@RequestBody Live2DChatConfigDO config) {
        return live2DService.saveChatConfig(config);
    }

    @PostMapping("/chat/{modelId}/recognize")
    public Result<ChatResponse> recognizeImage(@PathVariable Long modelId,
                                                @RequestParam("image") MultipartFile image,
                                                @RequestParam(required = false, defaultValue = "") String question) {
        return live2DService.recognizeImage(modelId, image, question);
    }

    @PostMapping("/chat/{modelId}/generate")
    public Result<ChatResponse> generateImage(@PathVariable Long modelId,
                                               @RequestBody Map<String, String> body) {
        String prompt = body.get("prompt");
        if (prompt == null || prompt.isBlank()) return Result.fail("描述不能为空");
        return live2DService.generateImage(modelId, prompt);
    }
}