package org.riteshingle.campusgig.Service;

import lombok.RequiredArgsConstructor;
import org.riteshingle.campusgig.Enum.ContractStatus;
import org.riteshingle.campusgig.Enum.ProgressStatus;
import org.riteshingle.campusgig.Enum.Roles;
import org.riteshingle.campusgig.Model.*;
import org.riteshingle.campusgig.Repository.ContractRepository;
import org.riteshingle.campusgig.ResponseDTO.ContractDetailsResponseDTO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ContractService {
    private final ContractRepository contractRepository;
    private final AuthService authService;

    public Contract createContract(Job job, JobApplication jobApplication){
        if(job == null) throw new RuntimeException("Job is null..");
        if(jobApplication == null) throw new RuntimeException("Job application is null..");

        return Contract.builder()
                .contractStatus(ContractStatus.ACTIVE)
                .jobApplication(jobApplication)
                .job(job)
                .progressStatus(ProgressStatus.NOT_STARTED)
                .expectedDeliveryDate(jobApplication.getDeliveryDate())
                .agreementAmount(jobApplication.getBidAmount())
                .client(job.getUser())
                .gig(jobApplication.getGig())
                .build();
    }

    public void setProgress(Long contractId,String progressStatus){
        Contract contract = contractRepository.findById(contractId).orElseThrow(() -> new RuntimeException("Contract not found by ID : " + contractId));
        UserEntity currentProfile = authService.getCurrentProfile();
        GIG gig = currentProfile.getGig();

        if(gig == null) throw new RuntimeException("Only GIG can Change the project progress..");

        if(!contract.getContractStatus().equals(ContractStatus.ACTIVE))
            throw new RuntimeException("You can't do change in contract because contract is : "+contract.getContractStatus().name());

        ProgressStatus progress;
        try { progress = ProgressStatus.valueOf(progressStatus.trim().toUpperCase()); }
        catch (Exception e) {throw new RuntimeException("Invalid Progress status..");}

        validateStatusTransition(contract.getProgressStatus(),progress);

        contract.setProgressStatus(progress);
        contractRepository.save(contract);
    }

    public List<ContractDetailsResponseDTO> getContracts(){
        UserEntity currentProfile = authService.getCurrentProfile();

        if(!currentProfile.getIsVerified()){
            throw new RuntimeException("User is not verified...");
        }

        List<Contract> contracts = contractRepository.findMyContracts(currentProfile);
        return contracts.stream().map(this::contractDetailsResponseDTO).toList();
    }

    public ContractDetailsResponseDTO getContract(Long contractId){
        Contract contract = contractRepository.findById(contractId).orElseThrow(() -> new RuntimeException("Contract not found by ID : " + contractId));
        UserEntity currentProfile = authService.getCurrentProfile();

        if(currentProfile.getRoles().equals(Roles.USER)){
            throw new RuntimeException("You are not authorized to check contract..");
        }

        boolean isGig = currentProfile.getGig() != null && contract.getGig().getUser().getId().equals(currentProfile.getId());
        boolean isClient = contract.getClient().getId().equals(currentProfile.getId());

        if (!isClient && !isGig)
            throw new RuntimeException("You are not a participant of this conversation");

        return contractDetailsResponseDTO(contract);
    }

    public void closeContract(Long contractId){
        Contract contract = contractRepository.findById(contractId).orElseThrow(() -> new RuntimeException("Contract not found by ID : " + contractId));
        UserEntity currentProfile = authService.getCurrentProfile();

        if(!currentProfile.getRoles().contains(Roles.CLIENT)){
            throw new RuntimeException("You are not authorized update contract..");
        }

        if(contract.getContractStatus().equals(ContractStatus.CLOSED)){
            throw new RuntimeException("Contract is Already closed..");
        }

        if(!contract.getClient().getId().equals(currentProfile.getId())){
            throw new RuntimeException("You are not authorized update contract..");
        }

        contract.setContractStatus(ContractStatus.CLOSED);
        contractRepository.save(contract);
    }

//   Helper methods
    private ContractDetailsResponseDTO contractDetailsResponseDTO(Contract contract){
        String client = contract.getClient().getFirstName()+contract.getClient().getLastName();
        String gig = contract.getGig().getUser().getFirstName()+contract.getGig().getUser().getLastName();
        return ContractDetailsResponseDTO.builder()
                .contractId(contract.getId())
                .gigName(gig)
                .client(client)
                .agreementAmount(contract.getAgreementAmount())
                .conversationId(contract.getConversation().getId())
                .status(contract.getContractStatus())
                .deadline(contract.getExpectedDeliveryDate())
                .jobTitle(contract.getJob().getTitle())
                .progressStatus(contract.getProgressStatus())
                .build();
    }

    private void validateStatusTransition(ProgressStatus current, ProgressStatus next) {
        if (current == next) {
            throw new RuntimeException("Progress status is already " + current);
        }

        if (current == ProgressStatus.NOT_STARTED && next == ProgressStatus.COMPLETED) {
            throw new RuntimeException("Cannot mark as COMPLETED directly. Move to IN_PROGRESS first.");
        }

        if (current == ProgressStatus.IN_PROGRESS && next == ProgressStatus.NOT_STARTED) {
            throw new RuntimeException("Cannot move back to NOT_STARTED once IN_PROGRESS.");
        }

        if (current == ProgressStatus.COMPLETED) {
            throw new RuntimeException("Cannot change progress status once COMPLETED.");
        }
    }
}
