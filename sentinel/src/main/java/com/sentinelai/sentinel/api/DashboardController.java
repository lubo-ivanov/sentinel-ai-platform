package com.sentinelai.sentinel.api;

import com.sentinelai.sentinel.service.IncidentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final IncidentService incidentService;

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("incidents", incidentService.findAll());
        return "index";
    }


    @GetMapping("incidents/{id}")
    public String detail(@PathVariable UUID id, Model model) {
        incidentService.findById(id).ifPresent(i -> model.addAttribute("incident", i));
        return "incident";
    }
}
