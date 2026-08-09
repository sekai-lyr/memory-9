package com.sekai.sekai_form.mapper;

import com.sekai.sekai_form.dataobject.Live2DModelDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface Live2DModelMapper {
    List<Live2DModelDO> listAll();
    Live2DModelDO findById(@Param("id") Long id);
    int insert(Live2DModelDO model);
    int update(Live2DModelDO model);
    int deleteById(@Param("id") Long id);
}