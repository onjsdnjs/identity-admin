package io.spring.identityadmin.admin.monitoring.controller;

import io.spring.identityadmin.admin.monitoring.service.DashboardService;
import io.spring.identityadmin.admin.monitoring.dto.DashboardDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {

        DashboardDto dashboardData = dashboardService.getDashboardData();

        model.addAttribute("activePage", "dashboard");
        model.addAttribute("dashboardData", dashboardData);
        return "admin/dashboard";
    }
}
