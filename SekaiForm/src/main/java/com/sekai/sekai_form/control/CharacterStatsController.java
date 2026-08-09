package com.sekai.sekai_form.control;

import com.sekai.sekai_form.dataobject.CharacterStatsDO;
import com.sekai.sekai_form.dataobject.SekaiFormCharacterDO;
import com.sekai.sekai_form.model.Result;
import com.sekai.sekai_form.service.CharacterStatsService;
import com.sekai.sekai_form.service.SekaiFormCharacterService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/char-stats")
public class CharacterStatsController {
    private final CharacterStatsService statsService;
    private final SekaiFormCharacterService characterService;
    public CharacterStatsController(CharacterStatsService statsService, SekaiFormCharacterService characterService) {
        this.statsService = statsService; this.characterService = characterService;
    }

    @GetMapping("/{characterId}") public Result<CharacterStatsDO> getStats(@PathVariable Long characterId) {
        return Result.ok(statsService.getStats(characterId));
    }

    @PostMapping("/{characterId}/exp") public Result<SekaiFormCharacterDO> gainExp(@PathVariable Long characterId, @RequestBody Map<String, Integer> body) {
        SekaiFormCharacterDO c = characterService.getById(characterId).getData();
        if (c == null) return Result.fail("角色不存在");
        int exp = body.getOrDefault("amount", 0);
        statsService.gainExp(c, exp);
        return Result.ok(c);
    }

    @PostMapping("/{characterId}/allocate") public Result<SekaiFormCharacterDO> allocate(@PathVariable Long characterId, @RequestBody Map<String, String> body) {
        SekaiFormCharacterDO c = characterService.getById(characterId).getData();
        if (c == null) return Result.fail("角色不存在");
        String stat = body.get("stat");
        if (stat == null) return Result.fail("参数错误");
        boolean ok = statsService.allocatePoint(c, stat);
        if (!ok) return Result.fail("没有可用属性点");
        return Result.ok(c);
    }

    @PostMapping("/{characterId}/reset") public Result<SekaiFormCharacterDO> reset(@PathVariable Long characterId) {
        SekaiFormCharacterDO c = characterService.getById(characterId).getData();
        if (c == null) return Result.fail("角色不存在");
        statsService.resetStats(c);
        return Result.ok(c);
    }

    @PostMapping("/{characterId}/affection") public Result<SekaiFormCharacterDO> saveAffection(@PathVariable Long characterId, @RequestBody Map<String, Integer> body) {
        SekaiFormCharacterDO c = characterService.getById(characterId).getData();
        if (c == null) return Result.fail("角色不存在");
        Integer affection = body.get("affection");
        if (affection != null) {
            c.setAffection(affection);
            characterService.updateAffection(characterId, affection);
        }
        return Result.ok(c);
    }
}