package io.spring.identityadmin.admin.monitoring.controller;

import io.spring.identityadmin.admin.monitoring.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping({"", "/", "/dashboard"})
    public String dashboard(Model model) {
        model.addAttribute("dashboardData", dashboardService.getDashboardData());
        model.addAttribute("activePage", "dashboard");
        return "admin/dashboard";
    }
}