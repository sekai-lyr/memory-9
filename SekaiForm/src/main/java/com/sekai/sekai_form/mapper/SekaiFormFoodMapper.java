package com.sekai.sekai_form.mapper;

import com.sekai.sekai_form.dataobject.SekaiFormFoodDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface SekaiFormFoodMapper {
    List<SekaiFormFoodDO> listAll();
    SekaiFormFoodDO findById(@Param("id") Long id);
    int insert(SekaiFormFoodDO food);
    int deleteById(@Param("id") Long id);
}