package org.riteshingle.campusgig.Controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.riteshingle.campusgig.Model.UserEntity;
import org.riteshingle.campusgig.RequestDTO.ContractCancelOrWithdrawnRequestDTO;
import org.riteshingle.campusgig.ResponseDTO.ContractDetailsResponseDTO;
import org.riteshingle.campusgig.Service.AuthService;
import org.riteshingle.campusgig.Service.ContractService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/contract")
@RequiredArgsConstructor
public class ContractController {
    private final ContractService contractService;
    private final AuthService authService;

//    Contracts
    @PreAuthorize("hasRole('CLIENT') or hasRole('GIG')")
    @GetMapping("/contracts")
    public ResponseEntity<List<ContractDetailsResponseDTO>> getContracts(@RequestParam(required = false,defaultValue = "1")int page,
                                                                         @RequestParam(required = false,defaultValue = "10")int size,
                                                                         @RequestParam(required = false,defaultValue = "ASC")String direction,
                                                                         @RequestParam(required = false,defaultValue = "createdAt")String field,
                                                                         @RequestParam(required = false) String keyword,
                                                                         @RequestParam(required = false)LocalDate from,
                                                                         @RequestParam(required = false)LocalDate to){
        return ResponseEntity.ok(contractService.getContracts(page,size,direction,field,keyword,from,to));
    }

//    Contract
    @PreAuthorize("hasRole('CLIENT') or hasRole('GIG')")
    @GetMapping("/contract/{contractId}")
    public ResponseEntity<ContractDetailsResponseDTO> getContract(@PathVariable Long contractId){
        return ResponseEntity.ok(contractService.getContract(contractId));
    }

//    Progress
    @PreAuthorize("hasRole('GIG')")
    @PatchMapping("/progress")
    public ResponseEntity<ContractDetailsResponseDTO> updateProgress(@RequestParam Long contractId,@RequestParam String progress){
        UserEntity currentProfile = authService.getCurrentProfile();
        return ResponseEntity.ok(contractService.setProgress(contractId,progress,currentProfile.getId(),currentProfile));
    }

//    Complete Contract
    @PreAuthorize("hasRole('CLIENT')")
    @PatchMapping("/complete-contract/{contractId}")
    public ResponseEntity<ContractDetailsResponseDTO> completeContract(@PathVariable Long contractId){
        return ResponseEntity.ok(contractService.completeContract(contractId));
    }

//    Active Contract
    @PreAuthorize("hasRole('CLIENT')")
    @PatchMapping("/active-contract/{contractId}")
    public ResponseEntity<ContractDetailsResponseDTO> activeContract(@PathVariable Long contractId){
        return ResponseEntity.ok(contractService.activeContract(contractId));
    }

//    Cancel or Withdrawn Contract
    @PreAuthorize("hasRole('CLIENT') OR hasRole('GIG')")
    @PatchMapping("/break-contract/{contractId}")
    public ResponseEntity<ContractDetailsResponseDTO> cancelOrWithdrawnContract(@PathVariable Long contractId, @Valid @RequestBody ContractCancelOrWithdrawnRequestDTO dto){
        return ResponseEntity.ok(contractService.cancelOrWithdrawnContract(contractId,dto));
    }
}
