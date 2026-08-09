package com.sekai.sekai_form.service.impl;

import com.sekai.sekai_form.dataobject.SekaiFormInteractionLogDO;
import com.sekai.sekai_form.mapper.SekaiFormInteractionLogMapper;
import com.sekai.sekai_form.model.Result;
import com.sekai.sekai_form.service.SekaiFormInteractionService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class SekaiFormInteractionServiceImpl implements SekaiFormInteractionService {
    private final SekaiFormInteractionLogMapper logMapper;
    public SekaiFormInteractionServiceImpl(SekaiFormInteractionLogMapper logMapper) { this.logMapper = logMapper; }

    @Override
    public Result<List<SekaiFormInteractionLogDO>> listByCharacterId(Long characterId) { return Result.ok(logMapper.listByCharacterId(characterId)); }

    @Override
    public Result<SekaiFormInteractionLogDO> log(Long characterId, String type, String description) {
        SekaiFormInteractionLogDO log = new SekaiFormInteractionLogDO();
        log.setCharacterId(characterId); log.setInteractionType(type); log.setDescription(description);
        logMapper.insert(log);
        return Result.ok(log);
    }
}