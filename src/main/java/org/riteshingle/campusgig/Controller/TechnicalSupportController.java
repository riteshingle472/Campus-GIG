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
    public ResponseEntity<?> technicalSupport(@Valid @RequestBody TechnicalSupportRequestDTO dto){
        technicalSupportService.postTechnicalIssue(dto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("technical-supports")
    public ResponseEntity<List<TechnicalSupportResponseDTO>> myTechnicalReport(@RequestParam(defaultValue = "1") int pageNumber,
                                                                               @RequestParam(required = false,defaultValue = "10") int page,
                                                                               @RequestParam(required = false,defaultValue = "createdAt") String field,
                                                                               @RequestParam(required = false,defaultValue = "DESC") String direction){
        Pageable pageable = PageRequest.of(pageNumber - 1 , page, Sort.by(Sort.Direction.fromString(direction),field));
        return ResponseEntity.ok(technicalSupportService.getMyTechnicalReports(pageable));
    }

    @GetMapping("technical-support/{id}")
    public ResponseEntity<TechnicalSupportResponseDTO> myTechnicalIssue(@PathVariable Long id){
        return ResponseEntity.ok(technicalSupportService.getMyTechnicalIssueById(id));
    }
}
