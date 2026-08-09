package com.sekai.sekai_form.service;

import com.sekai.sekai_form.dataobject.CharacterStatsDO;
import com.sekai.sekai_form.dataobject.SekaiFormCharacterDO;

public interface CharacterStatsService {
    CharacterStatsDO getStats(Long characterId);
    void initStats(SekaiFormCharacterDO character);
    void updateStats(SekaiFormCharacterDO character);
    void gainExp(SekaiFormCharacterDO character, int exp);
    boolean allocatePoint(SekaiFormCharacterDO character, String stat);
    void resetStats(SekaiFormCharacterDO character);
}