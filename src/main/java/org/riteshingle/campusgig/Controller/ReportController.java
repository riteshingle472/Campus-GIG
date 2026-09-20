package org.riteshingle.campusgig.Controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.riteshingle.campusgig.RequestDTO.ReportRequestDTO;
import org.riteshingle.campusgig.ResponseDTO.ReportResponseDTO;
import org.riteshingle.campusgig.Service.ReportService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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

    //    Report
    @PreAuthorize("hasRole('CLIENT') or hasRole('GIG')")
    @GetMapping("/reports")
    public ResponseEntity<List<ReportResponseDTO>> reports(){
        return ResponseEntity.ok(reportService.reports());
    }
}