package com.sekai.sekai_form.control;

import com.sekai.sekai_form.dataobject.SekaiFormInteractionLogDO;
import com.sekai.sekai_form.model.Result;
import com.sekai.sekai_form.service.SekaiFormInteractionService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/interaction")
public class SekaiFormInteractionControl {
    private final SekaiFormInteractionService interactionService;
    public SekaiFormInteractionControl(SekaiFormInteractionService interactionService) { this.interactionService = interactionService; }

    @GetMapping("/{characterId}") public Result<List<SekaiFormInteractionLogDO>> list(@PathVariable Long characterId) {
        return interactionService.listByCharacterId(characterId);
    }

    @PostMapping public Result<SekaiFormInteractionLogDO> log(@RequestParam Long characterId, @RequestParam String type, @RequestParam(required = false) String description) {
        return interactionService.log(characterId, type, description != null ? description : "");
    }
}