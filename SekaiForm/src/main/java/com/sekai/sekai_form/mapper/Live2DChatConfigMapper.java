package com.sekai.sekai_form.mapper;

import com.sekai.sekai_form.dataobject.Live2DChatConfigDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface Live2DChatConfigMapper {
    Live2DChatConfigDO getByModelId(Long modelId);
    int insert(Live2DChatConfigDO config);
    int update(Live2DChatConfigDO config);
}