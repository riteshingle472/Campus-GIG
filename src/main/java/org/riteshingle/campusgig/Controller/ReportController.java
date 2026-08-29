package org.riteshingle.campusgig.Controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.riteshingle.campusgig.RequestDTO.ReportRequestDTO;
import org.riteshingle.campusgig.Service.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RequestMapping("/api/report")
@RestController
public class ReportController {
    private final ReportService reportService;

    @PreAuthorize("hasRole('CLIENT') or hasRole('GIG')")
    @PostMapping("/report")
    public ResponseEntity<?> report(@Valid @RequestBody ReportRequestDTO dto){
        reportService.report(dto);
        return ResponseEntity.noContent().build();
    }
}
