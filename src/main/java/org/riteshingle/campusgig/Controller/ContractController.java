package org.riteshingle.campusgig.Controller;

import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.riteshingle.campusgig.ResponseDTO.ContractDetailsResponseDTO;
import org.riteshingle.campusgig.Service.ContractService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/contract")
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
    @PostMapping("/progress")
    public ResponseEntity<?> updateProgress(@RequestParam Long contractId,@RequestParam String progress){
        contractService.setProgress(contractId,progress);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('CLIENT')")
    @PostMapping("/close-contract/{contractId}")
    public ResponseEntity<?> closeContract(@PathVariable Long contractId){
        contractService.closeContract(contractId);
        return ResponseEntity.noContent().build();
    }
}
