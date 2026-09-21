package com.sekai.sekai_form.service;

import com.sekai.sekai_form.dataobject.Live2DChatConfigDO;
import com.sekai.sekai_form.dataobject.Live2DDialogueDO;
import com.sekai.sekai_form.dataobject.Live2DModelDO;
import com.sekai.sekai_form.model.ChatResponse;
import com.sekai.sekai_form.model.Result;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;

public interface Live2DService {
    Result<List<Live2DModelDO>> listModels();
    Result<Live2DModelDO> getModel(Long id);
    Result<Live2DModelDO> uploadModel(MultipartFile file, String displayName);
    Result<Void> deleteModel(Long id);
    Result<List<Live2DDialogueDO>> listDialogues(Long modelId);
    Result<Live2DDialogueDO> getRandomDialogue(Long modelId, String category);
    Result<ChatResponse> chat(Long modelId, String message, List<Map<String, Object>> history);
    Result<ChatResponse> chat(Long modelId, String message, List<Map<String, Object>> history, String conversationId, String userId);
    Result<ChatResponse> recognizeImage(Long modelId, MultipartFile image, String question);
    Result<ChatResponse> recognizeImage(Long modelId, MultipartFile image, String question, String conversationId, String userId);
    Result<ChatResponse> generateImage(Long modelId, String prompt);
    Result<ChatResponse> generateImage(Long modelId, String prompt, String conversationId, String userId);
    Result<Live2DChatConfigDO> getChatConfig(Long modelId);
    Result<Void> saveChatConfig(Live2DChatConfigDO config);
    Result<ChatResponse> analyzeFile(Long modelId, MultipartFile file, String question, String conversationId, String userId);
    Result<ChatResponse> processAudio(Long modelId, MultipartFile audio, String question, String conversationId, String userId);
    Result<ChatResponse> editImage(Long modelId, MultipartFile image, String instruction, String conversationId, String userId);
}
