package io.spring.identityadmin.admin.iam.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String registerPage(Model model) {
        return "admin/login";
    }

}
