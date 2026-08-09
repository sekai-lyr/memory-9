package com.sekai.sekai_form.service;

import com.sekai.sekai_form.dataobject.SekaiFormUserDO;
import com.sekai.sekai_form.model.Result;

public interface SekaiFormUserService {
    Result<SekaiFormUserDO> register(String userName, String password, String email);
    Result<SekaiFormUserDO> login(String userName, String password);
    Result<SekaiFormUserDO> getUserById(Long id);
}