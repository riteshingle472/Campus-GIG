package org.riteshingle.campusgig.Controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.riteshingle.campusgig.RequestDTO.ReportRequestDTO;
import org.riteshingle.campusgig.Service.ReportService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.riteshingle.campusgig.ResponseDTO.ReportResponseDTO;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;

@RequiredArgsConstructor
@RequestMapping("/api/report")
@RestController
public class ReportController {
    private final ReportService reportService;

//    Report
    @PreAuthorize("hasRole('CLIENT') or hasRole('GIG')")
    @PostMapping("/report")
    public ResponseEntity<?> report(@Valid @RequestBody ReportRequestDTO dto){
        reportService.report(dto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

//    My Reports
    @PreAuthorize("hasRole('CLIENT') or hasRole('GIG')")
    @GetMapping("/reports")
    public ResponseEntity<List<ReportResponseDTO>> getMyReports(
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page - 1, size);
        return ResponseEntity.ok(reportService.getMyReports(pageable));
    }
}