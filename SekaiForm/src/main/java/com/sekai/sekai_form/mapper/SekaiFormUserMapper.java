package com.sekai.sekai_form.mapper;

import com.sekai.sekai_form.dataobject.SekaiFormUserDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SekaiFormUserMapper {
    SekaiFormUserDO findByUserName(@Param("userName") String userName);
    int insert(SekaiFormUserDO user);
    SekaiFormUserDO findById(@Param("id") Long id);
}