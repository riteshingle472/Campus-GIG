package org.riteshingle.campusgig.Controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.riteshingle.campusgig.RequestDTO.ContractCancelOrWithdrawnRequestDTO;
import org.riteshingle.campusgig.ResponseDTO.ContractDetailsResponseDTO;
import org.riteshingle.campusgig.Service.ContractService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contract")
@RequiredArgsConstructor
public class ContractController {
    private final ContractService contractService;

    @PreAuthorize("hasRole('CLIENT') or hasRole('GIG')")
    @GetMapping("/contracts")
    public ResponseEntity<List<ContractDetailsResponseDTO>> getContracts(){
        return ResponseEntity.ok(contractService.getContracts());
    }

    @PreAuthorize("hasRole('CLIENT') or hasRole('GIG')")
    @GetMapping("/contract/{contractId}")
    public ResponseEntity<ContractDetailsResponseDTO> getContract(@PathVariable Long contractId){
        return ResponseEntity.ok(contractService.getContract(contractId));
    }

    @PreAuthorize("hasRole('GIG')")
    @PatchMapping("/progress")
    public ResponseEntity<?> updateProgress(@RequestParam Long contractId,@RequestParam String progress){
        contractService.setProgress(contractId,progress);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('CLIENT')")
    @PatchMapping("/complete-contract/{contractId}")
    public ResponseEntity<?> completeContract(@PathVariable Long contractId){
        contractService.completeContract(contractId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('CLIENT')")
    @PatchMapping("/active-contract/{contractId}")
    public ResponseEntity<?> activeContract(@PathVariable Long contractId){
        contractService.activeContract(contractId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('CLIENT') OR hasRole('GIG')")
    @PatchMapping("/break-contract/{contractId}")
    public ResponseEntity<?> cancelOrWithdrawnContract(@PathVariable Long contractId, @Valid @RequestBody ContractCancelOrWithdrawnRequestDTO dto){
        contractService.cancelOrWithdrawnContract(contractId,dto);
        return ResponseEntity.noContent().build();
    }
}
