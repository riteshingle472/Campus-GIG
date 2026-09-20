package org.riteshingle.campusgig.Controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.riteshingle.campusgig.RequestDTO.ContractCancelOrWithdrawnRequestDTO;
import org.riteshingle.campusgig.ResponseDTO.ContractDetailsResponseDTO;
import org.riteshingle.campusgig.Service.ContractService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/contract")
@RequiredArgsConstructor
public class ContractController {
    private final ContractService contractService;

//    All Contracts
    @PreAuthorize("hasRole('CLIENT') or hasRole('GIG')")
    @GetMapping("/contracts")
    public ResponseEntity<List<ContractDetailsResponseDTO>> getContracts(@RequestParam(required = false,defaultValue = "1")int page,
                                                                         @RequestParam(required = false,defaultValue = "10")int size,
                                                                         @RequestParam(required = false,defaultValue = "ASC")String direction,
                                                                         @RequestParam(required = false,defaultValue = "createdAt")String field,
                                                                         @RequestParam(required = false) String keyword,
                                                                         @RequestParam(required = false)LocalDate from,
                                                                         @RequestParam(required = false)LocalDate to){
        Pageable pageable = PageRequest.of(page-1, size, Sort.Direction.fromString(direction),field);
        return ResponseEntity.ok(contractService.getContracts(pageable,keyword,from,to));
    }

//    Contract by ID
    @PreAuthorize("hasRole('CLIENT') or hasRole('GIG')")
    @GetMapping("/contract/{contractId}")
    public ResponseEntity<ContractDetailsResponseDTO> getContract(@PathVariable Long contractId){
        return ResponseEntity.ok(contractService.getContract(contractId));
    }

//    Set Work Progress ( ) -> GIG
    @PreAuthorize("hasRole('GIG')")
    @PatchMapping("/progress")
    public ResponseEntity<?> updateProgress(@RequestParam Long contractId,@RequestParam String progress){
        contractService.setProgress(contractId,progress);
        return ResponseEntity.noContent().build();
    }

//    Complete Contract ( ) -> Client
    @PreAuthorize("hasRole('CLIENT')")
    @PatchMapping("/complete-contract/{contractId}")
    public ResponseEntity<?> completeContract(@PathVariable Long contractId){
        contractService.completeContract(contractId);
        return ResponseEntity.noContent().build();
    }

//    Activate Contract ( ) -> Client
    @PreAuthorize("hasRole('CLIENT')")
    @PatchMapping("/active-contract/{contractId}")
    public ResponseEntity<?> activeContract(@PathVariable Long contractId){
        contractService.activeContract(contractId);
        return ResponseEntity.noContent().build();
    }

//    Break Contract
    @PreAuthorize("hasRole('CLIENT') OR hasRole('GIG')")
    @PatchMapping("/break-contract/{contractId}")
    public ResponseEntity<?> cancelOrWithdrawnContract(@PathVariable Long contractId, @Valid @RequestBody ContractCancelOrWithdrawnRequestDTO dto){
        contractService.cancelOrWithdrawnContract(contractId,dto);
        return ResponseEntity.noContent().build();
    }
}
