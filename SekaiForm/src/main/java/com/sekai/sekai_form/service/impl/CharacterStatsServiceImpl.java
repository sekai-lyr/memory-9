package com.sekai.sekai_form.service.impl;

import com.sekai.sekai_form.dataobject.CharacterStatsDO;
import com.sekai.sekai_form.dataobject.SekaiFormCharacterDO;
import com.sekai.sekai_form.service.CharacterStatsService;
import org.springframework.stereotype.Service;

@Service
public class CharacterStatsServiceImpl implements CharacterStatsService {
    @Override
    public CharacterStatsDO getStats(Long characterId) {
        CharacterStatsDO s = new CharacterStatsDO(); s.setCharacterId(characterId); s.setLevel(1); s.setExp(0);
        s.setHp(100); s.setAtk(20); s.setDef(10); s.setFreePoints(0); return s;
    }

    @Override
    public void initStats(SekaiFormCharacterDO character) {
        character.setLevel(1); character.setExp(0); character.setHp(100); character.setAtk(20); character.setDef(10);
        character.setFreePoints(0);
    }

    @Override
    public void updateStats(SekaiFormCharacterDO character) { /* stats are on character DO directly */ }

    @Override
    public void gainExp(SekaiFormCharacterDO character, int exp) {
        int current = character.getExp() != null ? character.getExp() : 0;
        int level = character.getLevel() != null ? character.getLevel() : 1;
        current += exp;
        int needed = level * 100;
        while (current >= needed) {
            current -= needed; level++;
            character.setLevel(level); character.setFreePoints((character.getFreePoints() != null ? character.getFreePoints() : 0) + 3);
            needed = level * 100;
        }
        character.setExp(current);
    }

    @Override
    public boolean allocatePoint(SekaiFormCharacterDO character, String stat) {
        int pts = character.getFreePoints() != null ? character.getFreePoints() : 0;
        if (pts <= 0) return false;
        character.setFreePoints(pts - 1);
        switch (stat.toUpperCase()) {
            case "HP": character.setHp((character.getHp() != null ? character.getHp() : 100) + 10); break;
            case "ATK": character.setAtk((character.getAtk() != null ? character.getAtk() : 20) + 5); break;
            case "DEF": character.setDef((character.getDef() != null ? character.getDef() : 10) + 3); break;
            default: return false;
        }
        return true;
    }

    @Override
    public void resetStats(SekaiFormCharacterDO character) {
        character.setLevel(1); character.setExp(0); character.setFreePoints(0);
        character.setHp(100); character.setAtk(20); character.setDef(10);
        character.setAffection(0);
    }
}