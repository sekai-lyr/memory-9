package com.sekai.sekai_form.mapper;

import com.sekai.sekai_form.dataobject.SekaiFormCharacterDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface SekaiFormCharacterMapper {
    List<SekaiFormCharacterDO> listByUserId(@Param("userId") Long userId);
    SekaiFormCharacterDO findById(@Param("id") Long id);
    int insert(SekaiFormCharacterDO character);
    int update(SekaiFormCharacterDO character);
    int deleteById(@Param("id") Long id);
}