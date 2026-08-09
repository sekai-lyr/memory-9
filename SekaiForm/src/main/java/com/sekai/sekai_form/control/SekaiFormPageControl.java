package com.sekai.sekai_form.control;

import com.sekai.sekai_form.dataobject.SekaiFormUserDO;
import com.sekai.sekai_form.model.Result;
import com.sekai.sekai_form.service.SekaiFormUserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class SekaiFormPageControl {
    private final SekaiFormUserService userService;
    public SekaiFormPageControl(SekaiFormUserService userService) { this.userService = userService; }

    @GetMapping("/live2d") public String live2d() { return "live2d"; }

    @GetMapping("/") public String home() { return "redirect:/live2d"; }

        @GetMapping("/login") public String loginPage(HttpSession session) {
        if (session.getAttribute("loginUser") != null) return "redirect:/";
        return "login";
    }

    @PostMapping("/login") public String doLogin(@RequestParam String userName, @RequestParam String password, HttpSession session, Model model) {
        Result<SekaiFormUserDO> result = userService.login(userName, password);
        if (result.isSuccess()) { session.setAttribute("loginUser", result.getData()); return "redirect:/"; }
        model.addAttribute("error", result.getMessage()); return "login";
    }

    @GetMapping("/logout") public String logout(HttpSession session) { session.invalidate(); return "redirect:/"; }
}