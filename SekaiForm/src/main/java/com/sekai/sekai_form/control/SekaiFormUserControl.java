package com.sekai.sekai_form.control;

import com.sekai.sekai_form.dataobject.SekaiFormUserDO;
import com.sekai.sekai_form.model.Result;
import com.sekai.sekai_form.service.SekaiFormUserService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class SekaiFormUserControl {
    private final SekaiFormUserService userService;
    public SekaiFormUserControl(SekaiFormUserService userService) { this.userService = userService; }

    @PostMapping("/register") public Result<SekaiFormUserDO> register(@RequestParam String userName, @RequestParam String password, @RequestParam(required = false) String email) {
        return userService.register(userName, password, email);
    }

    @PostMapping("/login") public Result<SekaiFormUserDO> login(@RequestParam String userName, @RequestParam String password) { return userService.login(userName, password); }

    @GetMapping("/{id}") public Result<SekaiFormUserDO> getById(@PathVariable Long id) { return userService.getUserById(id); }
}