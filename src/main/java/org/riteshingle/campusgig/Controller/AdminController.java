package org.riteshingle.campusgig.Controller;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.riteshingle.campusgig.RequestDTO.AdminAuthDTO;
import org.riteshingle.campusgig.RequestDTO.AdminSendMailRequestDTO;
import org.riteshingle.campusgig.ResponseDTO.*;
import org.riteshingle.campusgig.Service.AdminService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final AdminService adminService;

//    Register Admin
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AdminAuthDTO adminAuthDTO) {
        adminService.register(adminAuthDTO);
        return ResponseEntity.noContent().build();
    }

//    Login Admin
    @GetMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody AdminAuthDTO adminAuthDTO, HttpServletResponse response) {
        return ResponseEntity.ok(adminService.login(adminAuthDTO, response));
    }

//    Stats Card
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/stats")
    public ResponseEntity<AdminDashboardCardStatsResponseDTO> stats(@RequestParam(required = false) LocalDate from,
                                                                    @RequestParam(required = false) LocalDate to,
                                                                    @RequestParam(required = false,defaultValue = "APPLIED") String jobApplicationStatus,
                                                                    @RequestParam(required = false,defaultValue = "OPEN") String jobStatus,
                                                                    @RequestParam(required = false,defaultValue = "ACTIVE") String contractStatus,
                                                                    @RequestParam(required = false,defaultValue = "PENDING") String reportStatus){
        return ResponseEntity.ok(adminService.dashboardCardStats(from, to,jobApplicationStatus,jobStatus,contractStatus,reportStatus));
    }

//    Most Popular Job
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/popular-job")
    public ResponseEntity<List<AdminDashboardMostPopularJobResponseDTO>> mostPopularJob(@RequestParam(required = false) LocalDate from,
                                                                                        @RequestParam(required = false) LocalDate to) {
        return ResponseEntity.ok(adminService.mostPopularJob(from, to));
    }

//    Growth Chart
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/growth-chart")
    public ResponseEntity<List<GrowthChartResponseDTO>> growthChart(@RequestParam(required = false) LocalDate from,
                                                                    @RequestParam(required = false) LocalDate to) {
        return ResponseEntity.ok(adminService.growthChart(from,to));
    }

//    Admin Mail Service
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/mail")
    public ResponseEntity<?> sendMail(@RequestBody AdminSendMailRequestDTO requestDTO) {
        adminService.adminMail(requestDTO);
        return ResponseEntity.noContent().build();
    }

    //    GIGs API
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/gigs")
    public ResponseEntity<List<AdminGigResponseDTO>> gigs(@RequestParam(required = false, defaultValue = "1") int pageNumber,
                                                          @RequestParam(required = false, defaultValue = "10") int pageSize,
                                                          @RequestParam(required = false, defaultValue = "ASC") String direction,
                                                          @RequestParam(required = false, defaultValue = "createdAt") String field) {
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize, Sort.Direction.fromString(direction), field);
        return ResponseEntity.ok(adminService.gigs(pageable));
    }

    //    GIG API
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/gig")
    public ResponseEntity<AdminGigResponseDTO> gig(@RequestParam Long id) {
        return ResponseEntity.ok(adminService.gig(id));
    }

    //    Clients API
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/clients")
    public ResponseEntity<List<AdminUserAndClientResponseDTO>> clients(@RequestParam(required = false, defaultValue = "1") int pageNumber,
                                                                       @RequestParam(required = false, defaultValue = "10") int pageSize,
                                                                       @RequestParam(required = false, defaultValue = "ASC") String direction,
                                                                       @RequestParam(required = false, defaultValue = "createdAt") String field) {
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize, Sort.Direction.fromString(direction), field);
        return ResponseEntity.ok(adminService.clients(pageable));
    }

    //    Client API
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/client")
    public ResponseEntity<AdminUserAndClientResponseDTO> client(@RequestParam Long id) {
        return ResponseEntity.ok(adminService.client(id));
    }

    //    job applications API
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/job-applications")
    public ResponseEntity<List<AdminJobApplicationListResponseDTO>> jobApplications(@RequestParam(required = false, defaultValue = "1") int pageNumber,
                                                                                    @RequestParam(required = false, defaultValue = "10") int pageSize,
                                                                                    @RequestParam(required = false, defaultValue = "ASC") String direction,
                                                                                    @RequestParam(required = false, defaultValue = "createdAt") String field) {
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize, Sort.Direction.fromString(direction), field);
        return ResponseEntity.ok(adminService.jobApplications(pageable));
    }

    //    Job application API
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/job-application")
    public ResponseEntity<AdminJobApplicationResponseDTO> jobApplication(@RequestParam Long id) {
        return ResponseEntity.ok(adminService.jobApplication(id));
    }

    //    Jobs API
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/jobs")
    public ResponseEntity<List<AdminJobResponseDTO>> jobs(@RequestParam(required = false, defaultValue = "1") int pageNumber,
                                                          @RequestParam(required = false, defaultValue = "10") int pageSize,
                                                          @RequestParam(required = false, defaultValue = "ASC") String direction,
                                                          @RequestParam(required = false, defaultValue = "publishAt") String field) {
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize, Sort.Direction.fromString(direction), field);
        return ResponseEntity.ok(adminService.jobs(pageable));
    }

    //    Job API
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/job")
    public ResponseEntity<AdminJobResponseDTO> job(@RequestParam Long id) {
        return ResponseEntity.ok(adminService.job(id));
    }

    //    Report API
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/report")
    public ResponseEntity<ReportResponseDTO> report(@RequestParam Long id) {
        return ResponseEntity.ok(adminService.report(id));
    }

    //    Reports API
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/reports")
    public ResponseEntity<List<AdminReportListResponseDTO>> reports(@RequestParam(required = false, defaultValue = "1") int pageNumber,
                                                                    @RequestParam(required = false, defaultValue = "10") int pageSize,
                                                                    @RequestParam(required = false, defaultValue = "ASC") String direction,
                                                                    @RequestParam(required = false, defaultValue = "createdAt") String field) {
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize, Sort.Direction.fromString(direction), field);
        return ResponseEntity.ok(adminService.reports(pageable));
    }
}
