package io.spring.identityadmin.admin.controller;

import io.spring.identityadmin.admin.service.impl.DashboardService;
import io.spring.identityadmin.domain.dto.DashboardDto;
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
public class WorkbenchController {

    private final DashboardService dashboardService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {

        DashboardDto dashboardData = dashboardService.getDashboardData();

        model.addAttribute("activePage", "dashboard");
        model.addAttribute("dashboardData", dashboardData);
        return "admin/dashboard";
    }

    @GetMapping("/workbench")
    public String workbench(Model model) {
        model.addAttribute("activePage", "workbench");
        return "admin/workbench";
    }
}
