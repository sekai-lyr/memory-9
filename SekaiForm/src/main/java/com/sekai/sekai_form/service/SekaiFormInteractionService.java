package com.sekai.sekai_form.service;

import com.sekai.sekai_form.dataobject.SekaiFormInteractionLogDO;
import com.sekai.sekai_form.model.Result;
import java.util.List;

public interface SekaiFormInteractionService {
    Result<List<SekaiFormInteractionLogDO>> listByCharacterId(Long characterId);
    Result<SekaiFormInteractionLogDO> log(Long characterId, String type, String description);
}