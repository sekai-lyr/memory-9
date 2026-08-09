package com.sekai.sekai_form.service.impl;

import com.sekai.sekai_form.dataobject.SekaiFormCharacterDO;
import com.sekai.sekai_form.mapper.SekaiFormCharacterMapper;
import com.sekai.sekai_form.model.CharacterForm;
import com.sekai.sekai_form.model.FeedForm;
import com.sekai.sekai_form.model.FoodForm;
import com.sekai.sekai_form.model.Result;
import com.sekai.sekai_form.service.CharacterStatsService;
import com.sekai.sekai_form.service.SekaiFormCharacterService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class SekaiFormCharacterServiceImpl implements SekaiFormCharacterService {
    private final SekaiFormCharacterMapper characterMapper;
    private final CharacterStatsService statsService;
    public SekaiFormCharacterServiceImpl(SekaiFormCharacterMapper characterMapper, CharacterStatsService statsService) {
        this.characterMapper = characterMapper; this.statsService = statsService;
    }

    @Override
    public Result<List<SekaiFormCharacterDO>> listByUserId(Long userId) { return Result.ok(characterMapper.listByUserId(userId)); }

    @Override
    public Result<SekaiFormCharacterDO> getById(Long id) {
        SekaiFormCharacterDO c = characterMapper.findById(id);
        if (c == null) return Result.fail("角色不存在");
        return Result.ok(c);
    }

    @Override
    public Result<SekaiFormCharacterDO> create(Long userId, CharacterForm form) {
        SekaiFormCharacterDO c = new SekaiFormCharacterDO();
        c.setUserId(userId); c.setName(form.getName()); c.setDescription(form.getDescription());
        c.setModelType(form.getModelType()); c.setModelPath(form.getModelPath());
        c.setSatiety(50); c.setMood(50); c.setAffection(0); c.setLevel(1); c.setExp(0); c.setFreePoints(0);
        c.setHp(100); c.setAtk(20); c.setDef(10);
        characterMapper.insert(c);
        return Result.ok("创建成功", c);
    }

    @Override
    public Result<SekaiFormCharacterDO> update(Long id, CharacterForm form) {
        SekaiFormCharacterDO c = characterMapper.findById(id);
        if (c == null) return Result.fail("角色不存在");
        if (form.getName() != null) c.setName(form.getName());
        if (form.getDescription() != null) c.setDescription(form.getDescription());
        characterMapper.update(c);
        return Result.ok("更新成功", c);
    }

    @Override
    public Result<Void> delete(Long id) {
        characterMapper.deleteById(id);
        return Result.ok("删除成功", null);
    }

    @Override
    public Result<SekaiFormCharacterDO> feed(FeedForm form) {
        SekaiFormCharacterDO c = characterMapper.findById(form.getCharacterId());
        if (c == null) return Result.fail("角色不存在");
        c.setSatiety(Math.min(100, c.getSatiety() + (form.getFoodId() != null ? 10 : 5)));
        c.setMood(Math.min(100, c.getMood() + 5));
        c.setAffection(Math.min(9999, c.getAffection() + 2));
        characterMapper.update(c);
        return Result.ok("喂食成功", c);
    }

    @Override
    public Result<SekaiFormCharacterDO> gainExp(Long id, int exp) {
        SekaiFormCharacterDO c = characterMapper.findById(id);
        if (c == null) return Result.fail("角色不存在");
        statsService.gainExp(c, exp);
        characterMapper.update(c);
        return Result.ok("获得经验", c);
    }

    @Override
    public Result<SekaiFormCharacterDO> reset(Long id) {
        SekaiFormCharacterDO c = characterMapper.findById(id);
        if (c == null) return Result.fail("角色不存在");
        statsService.resetStats(c);
        characterMapper.update(c);
        return Result.ok("重置成功", c);
    }

    @Override
    public Result<SekaiFormCharacterDO> allocatePoint(Long id, String stat) {
        SekaiFormCharacterDO c = characterMapper.findById(id);
        if (c == null) return Result.fail("角色不存在");
        if (!statsService.allocatePoint(c, stat)) return Result.fail("没有可分配的属性点");
        characterMapper.update(c);
        return Result.ok("分配成功", c);
    }

    @Override
    public Result<SekaiFormCharacterDO> updateAffection(Long id, Integer affection) {
        SekaiFormCharacterDO c = characterMapper.findById(id);
        if (c == null) return Result.fail("角色不存在");
        c.setAffection(affection);
        characterMapper.update(c);
        return Result.ok("好感度已更新", c);
    }
}