package org.riteshingle.campusgig.Controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.riteshingle.campusgig.RequestDTO.TechnicalSupportRequestDTO;
import org.riteshingle.campusgig.ResponseDTO.TechnicalSupportResponseDTO;
import org.riteshingle.campusgig.Service.TechnicalSupportService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/technical-support")
public class TechnicalSupportController {
    private final TechnicalSupportService technicalSupportService;

    @PostMapping("technical-support")
    public ResponseEntity<TechnicalSupportResponseDTO> technicalSupport(@Valid @RequestBody TechnicalSupportRequestDTO dto){
        return ResponseEntity.ok(technicalSupportService.postTechnicalIssue(dto));
    }

    @GetMapping("technical-supports")
    public ResponseEntity<List<TechnicalSupportResponseDTO>> myTechnicalReport(@RequestParam(defaultValue = "1") int page,
                                                                               @RequestParam(required = false,defaultValue = "10") int size,
                                                                               @RequestParam(required = false,defaultValue = "createdAt") String field,
                                                                               @RequestParam(required = false,defaultValue = "DESC") String direction){
        return ResponseEntity.ok(technicalSupportService.getMyTechnicalReports(page,size,field,direction));
    }

    @GetMapping("technical-support/{id}")
    public ResponseEntity<TechnicalSupportResponseDTO> myTechnicalIssue(@PathVariable Long id){
        return ResponseEntity.ok(technicalSupportService.getMyTechnicalIssueById(id));
    }
}
