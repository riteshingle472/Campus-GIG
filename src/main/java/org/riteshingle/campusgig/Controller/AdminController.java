package org.riteshingle.campusgig.Controller;

import lombok.RequiredArgsConstructor;
import org.riteshingle.campusgig.ResponseDTO.AdminDashboardCardStatsResponseDTO;
import org.riteshingle.campusgig.ResponseDTO.AdminDashboardMostPopularJobResponseDTO;
import org.riteshingle.campusgig.ResponseDTO.GrowthChartResponseDTO;
import org.riteshingle.campusgig.Service.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final AdminService adminService;

//    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/stats")
    public ResponseEntity<AdminDashboardCardStatsResponseDTO> stats(@RequestParam(required = false)LocalDate from,
                                                                    @RequestParam(required = false)LocalDate to){
        return ResponseEntity.ok(adminService.dashboardCardStats(from,to));
    }

//    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/popular-job")
    public ResponseEntity<List<AdminDashboardMostPopularJobResponseDTO>> mostPopularJob(@RequestParam(required = false)LocalDate from,
                                                                                        @RequestParam(required = false)LocalDate to){
        return ResponseEntity.ok(adminService.mostPopularJob(from,to));
    }

//    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/growth-chart")
    public ResponseEntity<List<GrowthChartResponseDTO>> growthChart(@RequestParam(required = false)LocalDate from){
        return ResponseEntity.ok(adminService.growthChart(from));
    }
}
