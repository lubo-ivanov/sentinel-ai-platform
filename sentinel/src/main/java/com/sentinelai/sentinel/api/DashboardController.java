package com.sentinelai.sentinel.api;

import com.sentinelai.sentinel.service.IncidentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final IncidentService incidentService;

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("incidents", incidentService.findAll());
        return "index";
    }
}
