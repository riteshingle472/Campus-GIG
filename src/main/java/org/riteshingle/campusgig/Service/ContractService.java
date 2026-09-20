package org.riteshingle.campusgig.Service;

import lombok.RequiredArgsConstructor;
import org.riteshingle.campusgig.Enum.*;
import org.riteshingle.campusgig.Exception.*;
import org.riteshingle.campusgig.Model.*;
import org.riteshingle.campusgig.Repository.ContractRepository;
import org.riteshingle.campusgig.RequestDTO.ContractCancelOrWithdrawnRequestDTO;
import org.riteshingle.campusgig.ResponseDTO.ContractDetailsResponseDTO;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ContractService {
    private final ContractRepository contractRepository;
    private final AuthService authService;

//    Create Contract
    public Contract createContract(Job job, JobApplication jobApplication){
//        check Job application and Job
        if(job == null) throw new ResourceNotFoundException("Job is null..");
        if(jobApplication == null) throw new ResourceNotFoundException("Job application is null..");

//        Return response
        return Contract.builder()
                .contractStatus(ContractStatus.PENDING)
                .jobApplication(jobApplication)
                .job(job)
                .progressStatus(ProgressStatus.NOT_STARTED)
                .expectedDeliveryDate(jobApplication.getDeliveryDate())
                .agreementAmount(jobApplication.getBidAmount())
                .client(job.getClient())
                .gig(jobApplication.getGig())
                .build();
    }

//    Set Progress ( ) -> GIG
    public void setProgress(Long contractId,String progressStatus){
//        Get Contract by ID
        Contract contract = contractRepository.findById(contractId).orElseThrow(() -> new ResourceNotFoundException("Contract not found by ID : " + contractId));

//        Get Current Logged-in Profile
        UserEntity currentProfile = authService.getCurrentProfile();
//        Get User Role
        Roles roles = currentProfile.getRoles().iterator().next();

//        Check ( ) ->  GIG Role
        if(!roles.equals(Roles.GIG)) throw new ResourceNotFoundException("Only GIG can Change the project progress..");

//        Check ( ) ->  Contract is Active
        if(!contract.getContractStatus().equals(ContractStatus.ACTIVE))
            throw new InvalidStatusException("You can't do change in contract because contract is : "+contract.getContractStatus().name());

//        Get Progress Status
        ProgressStatus progress;
        try { progress = ProgressStatus.valueOf(progressStatus.trim().toUpperCase()); }
        catch (Exception e) {throw new InvalidStatusException("Invalid Progress status..");}

//        Validate Progress status
        validateStatusTransition(contract.getProgressStatus(),progress);

//        Set status and save in DB
        contract.setProgressStatus(progress);
        contractRepository.save(contract);
    }

//    All Contracts
    public List<ContractDetailsResponseDTO> getContracts(Pageable pageable, String keyword, LocalDate from,LocalDate to){
//       Get Current Logged-in Profile
        UserEntity currentProfile = authService.getCurrentProfile();

//        Check ( ) ->  Profile is verified
        if(!currentProfile.getIsVerified()){
            throw new ForbiddenException("User is not verified...");
        }

//        Verify From and To dates
        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException("From date cannot be after to date");
        }

        if (to != null && to.isAfter(LocalDate.now())) {
            throw new BadRequestException("To date cannot be in the future");
        }

        LocalDateTime startFrom = from == null ? LocalDate.now().atStartOfDay() : from.atStartOfDay();
        LocalDateTime endTo = to == null ? LocalDate.now().atTime(LocalTime.MAX) : to.atTime(LocalTime.MAX);

//        Contract status
        ContractStatus contractStatus = keyword == null ? null : ContractStatus.valueOf(keyword.trim().toUpperCase());
//        Get Contract
        List<Contract> contracts = contractRepository.findMyContracts(currentProfile,contractStatus,startFrom,endTo,pageable);
        return contracts.stream().map(this::contractDetailsResponseDTO).toList();
    }

//    Contract
    public ContractDetailsResponseDTO getContract(Long contractId){
//        Get Contract by Contract ID
        Contract contract = contractRepository.findById(contractId).orElseThrow(() -> new ResourceNotFoundException("Contract not found by ID : " + contractId));
//        Get Current Logged-in Profile
        UserEntity currentProfile = authService.getCurrentProfile();
//        Get User role
        Roles roles = currentProfile.getRoles().iterator().next();

//        Check
        if(roles.equals(Roles.USER))
            throw new ForbiddenException("You are not authorized to check contract..");

//        Verify GIG And Client Form Contract
        boolean isGig = currentProfile.getGig() != null && contract.getGig().getUser().getId().equals(currentProfile.getId());
        boolean isClient = contract.getClient().getId().equals(currentProfile.getId());

        if (!isClient && !isGig)
            throw new ForbiddenException("You are not a participant of this conversation");

        return contractDetailsResponseDTO(contract);
    }

//    Complete Contract
    public void completeContract(Long contractId){
//        Get Contract by Contract ID
        Contract contract = contractRepository.findById(contractId).orElseThrow(() -> new ResourceNotFoundException("Contract not found by ID : " + contractId));
//        Get Current Logged-in Profile
        UserEntity currentProfile = authService.getCurrentProfile();

//        Check ( ) -> Only Client can Complete Contract
        if(!currentProfile.getRoles().contains(Roles.CLIENT))
            throw new ForbiddenException("You are not authorized update contract..");

//        Check ( ) -> Contract mustn't be Compete Withdrawn and Cancel
        if(contract.getContractStatus().equals(ContractStatus.COMPLETE) || contract.getContractStatus().equals(ContractStatus.WITHDRAWN)  || contract.getContractStatus().equals(ContractStatus.CANCEL))
            throw new InvalidStatusException("Contract is Already "+contract.getContractStatus());

//        Check ( ) -> Check Authorization
        if(!contract.getClient().getId().equals(currentProfile.getId())){
            throw new ForbiddenException("You are not authorized update contract..");
        }

//        Check ( ) -> Check Contract Progress
        if(contract.getProgressStatus().equals(ProgressStatus.NOT_STARTED) || contract.getProgressStatus().equals(ProgressStatus.IN_PROGRESS)){
            throw new InvalidStatusException("Contract Work Progress is : "+contract.getProgressStatus().name());
        }

//        Set status Complete and Saving in DB
        contract.setContractStatus(ContractStatus.COMPLETE);
        contractRepository.save(contract);
    }

//    Activate Contract
    public void activeContract(Long contractId){
//        Get Contract by contract Id
        Contract contract = contractRepository.findById(contractId).orElseThrow(() -> new ResourceNotFoundException("Contract not found by ID : " + contractId));
//        Get current Logged-in Profile
        UserEntity currentProfile = authService.getCurrentProfile();

//        Check ( ) -> Role
        if(!currentProfile.getRoles().contains(Roles.CLIENT)){
            throw new ForbiddenException("You are not authorized update contract..");
        }

//        Check ( ) -> Only Pending Contract can Activate
        if(contract.getContractStatus().equals(ContractStatus.COMPLETE) || contract.getContractStatus().equals(ContractStatus.ACTIVE)){
            throw new InvalidStatusException("Contract is : "+contract.getContractStatus()+"..");
        }

        if(contract.getContractStatus().equals(ContractStatus.WITHDRAWN) || contract.getContractStatus().equals(ContractStatus.CANCEL) ){
            throw new InvalidStatusException("Contract is "+contract.getContractStatus()+" by "+contract.getActionInitiatedBy());
        }

//        Check ( ) -> Check Contract Authorization
        if(!contract.getClient().getId().equals(currentProfile.getId())){
            throw new ForbiddenException("You are not authorized update contract..");
        }

        if(contract.getProgressStatus().equals(ProgressStatus.IN_PROGRESS) || contract.getProgressStatus().equals(ProgressStatus.COMPLETED)){
            throw new InvalidStatusException("Contract Work is in  : "+contract.getProgressStatus().name());
        }

        contract.setContractStatus(ContractStatus.ACTIVE);
        contractRepository.save(contract);
    }

//    Cancel and Withdrawn Contract
    @Transactional
    public void cancelOrWithdrawnContract(Long contractId,ContractCancelOrWithdrawnRequestDTO dto) {
        // Find Contract
        Contract contract = contractRepository.findById(contractId).orElseThrow(() ->new ResourceNotFoundException("Contract not found by ID : " + contractId));

        // Current logged-in user
        UserEntity currentProfile = authService.getCurrentProfile();

        // Contract already completed
        if (contract.getContractStatus() == ContractStatus.COMPLETE) {
            throw new InvalidStatusException("Contract is already completed");
        }

        // Check actual relationship with contract
        boolean isClient = contract.getClient().getId().equals(currentProfile.getId());
        boolean isGig = contract.getGig().getUser().getId().equals(currentProfile.getId());

        // User is neither Client nor GIG of this contract
        if (contract.getContractStatus() == ContractStatus.PENDING) {
            if (isClient) {
                contract.setContractStatus(ContractStatus.WITHDRAWN);
                contract.setActionInitiatedBy(ActionInitiatedBy.CLIENT);
            } else if (isGig) {
                contract.setContractStatus(ContractStatus.WITHDRAWN);
                contract.setActionInitiatedBy(ActionInitiatedBy.GIG);
            } else throw new ForbiddenException("You are not authorized for this contract");
        } else if (contract.getContractStatus() == ContractStatus.ACTIVE) {
            if (isClient) {
                contract.setContractStatus(ContractStatus.CANCEL);
                contract.setActionInitiatedBy(ActionInitiatedBy.CLIENT);
            } else if (isGig) {
                contract.setContractStatus(ContractStatus.CANCEL);
                contract.setActionInitiatedBy(ActionInitiatedBy.GIG);
            } else {
                throw new ForbiddenException("You are not authorized for this contract"   );
            }
        } else {
            throw new InvalidStatusException("Contract cannot be cancelled in current status: " + contract.getContractStatus());
        }

        // Validate cancellation reason
        ContractCancelReason contractCancelReason;
        try {
            contractCancelReason = ContractCancelReason.valueOf(dto.getReason().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidStatusException("Invalid Cancel reason : " + dto.getReason());
        }

        // Set cancellation details
        contract.setCancelReason(contractCancelReason);
        contract.setCancelledAt(LocalDateTime.now());
        contract.setCancellationRemark(dto.getRemark());

        // Save changes
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
            throw new InvalidStatusException("Progress status is already " + current);
        }

        if (current == ProgressStatus.NOT_STARTED && next == ProgressStatus.COMPLETED) {
            throw new InvalidStatusException("Cannot mark as COMPLETED directly. Move to IN_PROGRESS first.");
        }

        if (current == ProgressStatus.IN_PROGRESS && next == ProgressStatus.NOT_STARTED) {
            throw new InvalidStatusException("Cannot move back to NOT_STARTED once IN_PROGRESS.");
        }

        if (current == ProgressStatus.COMPLETED) {
            throw new InvalidStatusException("Cannot change progress status once COMPLETED.");
        }
    }
}
