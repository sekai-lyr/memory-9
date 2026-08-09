package com.sekai.sekai_form.service.impl;

import com.sekai.sekai_form.dataobject.SekaiFormUserDO;
import com.sekai.sekai_form.mapper.SekaiFormUserMapper;
import com.sekai.sekai_form.model.Result;
import com.sekai.sekai_form.service.SekaiFormUserService;
import com.sekai.sekai_form.util.PasswordHashUtil;
import org.springframework.stereotype.Service;

@Service
public class SekaiFormUserServiceImpl implements SekaiFormUserService {
    private final SekaiFormUserMapper userMapper;
    public SekaiFormUserServiceImpl(SekaiFormUserMapper userMapper) { this.userMapper = userMapper; }

    @Override
    public Result<SekaiFormUserDO> register(String userName, String password, String email) {
        SekaiFormUserDO existing = userMapper.findByUserName(userName);
        if (existing != null) return Result.fail("用户名已存在");
        SekaiFormUserDO user = new SekaiFormUserDO();
        user.setUserName(userName);
        user.setPassword(PasswordHashUtil.hash(password));
        user.setEmail(email);
        userMapper.insert(user);
        return Result.ok("注册成功", user);
    }

    @Override
    public Result<SekaiFormUserDO> login(String userName, String password) {
        SekaiFormUserDO user = userMapper.findByUserName(userName);
        if (user == null) return Result.fail("用户不存在");
        if (!PasswordHashUtil.verify(password, user.getPassword())) return Result.fail("密码错误");
        return Result.ok("登录成功", user);
    }

    @Override
    public Result<SekaiFormUserDO> getUserById(Long id) {
        SekaiFormUserDO user = userMapper.findById(id);
        if (user == null) return Result.fail("用户不存在");
        return Result.ok(user);
    }
}