package com.sekai.sekai_form.mapper;

import com.sekai.sekai_form.dataobject.SekaiFormInteractionLogDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface SekaiFormInteractionLogMapper {
    List<SekaiFormInteractionLogDO> listByCharacterId(@Param("characterId") Long characterId);
    int insert(SekaiFormInteractionLogDO log);
}