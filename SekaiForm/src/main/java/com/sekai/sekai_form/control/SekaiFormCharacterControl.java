package com.sekai.sekai_form.control;

import com.sekai.sekai_form.dataobject.SekaiFormCharacterDO;
import com.sekai.sekai_form.dataobject.SekaiFormUserDO;
import com.sekai.sekai_form.model.CharacterForm;
import com.sekai.sekai_form.model.FeedForm;
import com.sekai.sekai_form.model.Result;
import com.sekai.sekai_form.service.SekaiFormCharacterService;
import com.sekai.sekai_form.service.SekaiFormInteractionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/character")
public class SekaiFormCharacterControl {
    private final SekaiFormCharacterService characterService;
    private final SekaiFormInteractionService interactionService;
    public SekaiFormCharacterControl(SekaiFormCharacterService characterService, SekaiFormInteractionService interactionService) {
        this.characterService = characterService; this.interactionService = interactionService;
    }

    private Long getUserId(HttpSession session) {
        SekaiFormUserDO user = (SekaiFormUserDO) session.getAttribute("loginUser");
        return user != null ? user.getId() : null;
    }

    @GetMapping public Result<List<SekaiFormCharacterDO>> list(HttpSession session) {
        Long userId = getUserId(session);
        if (userId == null) return Result.fail("请先登录");
        return characterService.listByUserId(userId);
    }

    @GetMapping("/{id}") public Result<SekaiFormCharacterDO> getById(@PathVariable Long id) { return characterService.getById(id); }

    @PostMapping public Result<SekaiFormCharacterDO> create(@RequestBody CharacterForm form, HttpSession session) {
        Long userId = getUserId(session);
        if (userId == null) return Result.fail("请先登录");
        return characterService.create(userId, form);
    }

    @PutMapping("/{id}") public Result<SekaiFormCharacterDO> update(@PathVariable Long id, @RequestBody CharacterForm form) { return characterService.update(id, form); }

    @DeleteMapping("/{id}") public Result<Void> delete(@PathVariable Long id) { return characterService.delete(id); }

    @PostMapping("/feed") public Result<SekaiFormCharacterDO> feed(@RequestBody FeedForm form) { return characterService.feed(form); }

    @PostMapping("/{id}/gain-exp") public Result<SekaiFormCharacterDO> gainExp(@PathVariable Long id, @RequestParam int exp) { return characterService.gainExp(id, exp); }

    @PostMapping("/{id}/reset") public Result<SekaiFormCharacterDO> reset(@PathVariable Long id) { return characterService.reset(id); }

    @PostMapping("/{id}/allocate") public Result<SekaiFormCharacterDO> allocate(@PathVariable Long id, @RequestParam String stat) { return characterService.allocatePoint(id, stat); }

    @GetMapping("/{id}/logs") public Result<?> logs(@PathVariable Long id) { return interactionService.listByCharacterId(id); }
}