package com.sekai.sekai_form.agent;

import com.sekai.sekai_form.control.Live2DController;
import com.sekai.sekai_form.model.ChatResponse;
import com.sekai.sekai_form.model.Result;
import com.sekai.sekai_form.service.AgentProgressService;
import com.sekai.sekai_form.service.Live2DService;
import com.sekai.sekai_form.service.MediaArtifactService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class Live2DControllerRouteTest {
    @Test void existingChatAndNewMultimodalRoutesDelegateToLive2dService() throws Exception {
        Live2DService service = mock(Live2DService.class);
        when(service.chat(anyLong(), anyString(), anyList(), anyString(), anyString()))
                .thenReturn(Result.ok(new ChatResponse("ok", "Idle")));
        when(service.recognizeImage(anyLong(), any(), anyString(), anyString(), anyString()))
                .thenReturn(Result.ok(new ChatResponse("image", "Idle")));
        when(service.generateImage(anyLong(), anyString(), anyString(), anyString()))
                .thenReturn(Result.ok(new ChatResponse("generated", "Idle")));
        when(service.editImage(anyLong(), any(), anyString(), anyString(), anyString()))
                .thenReturn(Result.ok(new ChatResponse("edited", "Idle")));
        MockMvc mvc = standaloneSetup(new Live2DController(service, new MediaArtifactService(), new AgentProgressService())).build();

        mvc.perform(post("/api/live2d/chat/1")
                        .contentType("application/json")
                        .content("{\"message\":\"你好\",\"history\":[]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reply").value("ok"));
        mvc.perform(multipart("/api/live2d/chat/1/recognize")
                        .file(new MockMultipartFile("image", "character.png", "image/png", new byte[]{1})))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reply").value("image"));
        mvc.perform(post("/api/live2d/chat/1/generate")
                        .contentType("application/json")
                        .content("{\"prompt\":\"生成角色\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reply").value("generated"));
        mvc.perform(multipart("/api/live2d/chat/1/edit")
                        .file(new MockMultipartFile("image", "character.png", "image/png", new byte[]{1}))
                        .param("instruction", "调整配色"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reply").value("edited"));

        verify(service).chat(anyLong(), anyString(), anyList(), anyString(), anyString());
        verify(service).recognizeImage(anyLong(), any(), anyString(), anyString(), anyString());
        verify(service).generateImage(anyLong(), anyString(), anyString(), anyString());
        verify(service).editImage(anyLong(), any(), anyString(), anyString(), anyString());
    }

    @Test void progressRouteReturnsSafeProgressEnvelope() throws Exception {
        Live2DService service = mock(Live2DService.class);
        MockMvc mvc = standaloneSetup(new Live2DController(service, new MediaArtifactService(), new AgentProgressService())).build();
        mvc.perform(get("/api/live2d/chat/1/progress"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("idle"));
    }
}
