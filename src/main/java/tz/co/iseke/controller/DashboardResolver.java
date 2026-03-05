package tz.co.iseke.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import tz.co.iseke.dto.DashboardStatsDTO;
import tz.co.iseke.service.DashboardService;

@Controller
@RequiredArgsConstructor
public class DashboardResolver {

    private final DashboardService dashboardService;

    @QueryMapping
    public DashboardStatsDTO dashboardStats() {
        return dashboardService.getDashboardStats();
    }
}
