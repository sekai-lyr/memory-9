package com.sekai.sekai_form.mapper;

import com.sekai.sekai_form.dataobject.Live2DDialogueDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface Live2DDialogueMapper {
    List<Live2DDialogueDO> listByModelId(@Param("modelId") Long modelId);
    List<Live2DDialogueDO> listByCategory(@Param("modelId") Long modelId, @Param("category") String category);
    int insert(Live2DDialogueDO dialogue);
    int update(Live2DDialogueDO dialogue);
    int deleteById(@Param("id") Long id);
}