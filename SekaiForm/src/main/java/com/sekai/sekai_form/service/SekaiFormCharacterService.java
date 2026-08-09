package com.sekai.sekai_form.service;

import com.sekai.sekai_form.dataobject.SekaiFormCharacterDO;
import com.sekai.sekai_form.model.CharacterForm;
import com.sekai.sekai_form.model.FeedForm;
import com.sekai.sekai_form.model.Result;
import java.util.List;

public interface SekaiFormCharacterService {
    Result<List<SekaiFormCharacterDO>> listByUserId(Long userId);
    Result<SekaiFormCharacterDO> getById(Long id);
    Result<SekaiFormCharacterDO> create(Long userId, CharacterForm form);
    Result<SekaiFormCharacterDO> update(Long id, CharacterForm form);
    Result<Void> delete(Long id);
    Result<SekaiFormCharacterDO> feed(FeedForm form);
    Result<SekaiFormCharacterDO> gainExp(Long id, int exp);
    Result<SekaiFormCharacterDO> reset(Long id);
    Result<SekaiFormCharacterDO> allocatePoint(Long id, String stat);
    Result<SekaiFormCharacterDO> updateAffection(Long id, Integer affection);
}