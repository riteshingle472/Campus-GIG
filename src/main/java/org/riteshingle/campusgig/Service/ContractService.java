package org.riteshingle.campusgig.Service;

import lombok.RequiredArgsConstructor;
import org.hibernate.annotations.processing.Find;
import org.riteshingle.campusgig.Enum.*;
import org.riteshingle.campusgig.Exception.*;
import org.riteshingle.campusgig.Model.*;
import org.riteshingle.campusgig.Repository.ContractRepository;
import org.riteshingle.campusgig.RequestDTO.ContractCancelOrWithdrawnRequestDTO;
import org.riteshingle.campusgig.ResponseDTO.ContractDetailsResponseDTO;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;

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
    private final NotificationService notificationService;

//    Create Contract
    public Contract createContract(Job job, JobApplication jobApplication){
        if(job == null) throw new ResourceNotFoundException("Job is null..");
        if(jobApplication == null) throw new ResourceNotFoundException("Job application is null..");

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

//    Set Progress
    public void setProgress(Long contractId, String progressStatus){
//        Get Contract by ID
        Contract contract = contractRepository.findById(contractId).orElseThrow(() -> new ResourceNotFoundException("Contract not found by ID : " + contractId));
//        Get Current Logged-in Profile
        UserEntity currentProfile = authService.getCurrentProfile();

//        Only GIG can set the Progress
        if(!currentProfile.getRoles().contains(Roles.GIG))
            throw new ForbiddenException("Only GIG can change the project progress..");

//        Check Contract must active for setting progress
        if(!contract.getContractStatus().equals(ContractStatus.ACTIVE))
            throw new InvalidStatusException("You can't do change in contract because contract is : "+contract.getContractStatus().name());

//        Validate Progress Status
        ProgressStatus progress;
        try { progress = ProgressStatus.valueOf(progressStatus.trim().toUpperCase()); }
        catch (Exception e) {throw new InvalidStatusException("Invalid Progress status..");}

//        Validate Progress
        validateStatusTransition(contract.getProgressStatus(),progress);

//        Setting Progress Status and Save in DB
        contract.setProgressStatus(progress);
        contractRepository.save(contract);
//        Sent notification
        notificationService.notify(
                contract.getClient(),
                NotificationType.PROGRESS_UPDATED,
                "Project Progress Updated",
                "The progress of \"" + contract.getJob().getTitle() + "\" has been updated to " + progress.name(),
                contract.getId()
        );
    }

//    Contracts
    public Page<ContractDetailsResponseDTO> getContracts(Pageable pageable, String keyword, LocalDate from, LocalDate to){
//        Get Current Logged-in Profile
        UserEntity currentProfile = authService.getCurrentProfile();

//        Check Profile is verified or not ?
        if(!currentProfile.getIsVerified()){
            throw new ForbiddenException("User is not verified...");
        }

//        From date can't be after To date
        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException("From date cannot be after to date");
        }

//        To date can't be in future
        if (to != null && to.isAfter(LocalDate.now())) {
            throw new BadRequestException("To date cannot be in the future");
        }

//        Set Dates
        LocalDateTime startFrom = from == null ? null : from.atStartOfDay();
        LocalDateTime endTo = to == null ? null : to.atTime(LocalTime.MAX);

//        Validate Contract Status
        ContractStatus contractStatus = null;
        if (keyword != null) {
            try {
                contractStatus = ContractStatus.valueOf(keyword.trim().toUpperCase());
            } catch (Exception e) {
                throw new InvalidStatusException("Invalid contract status: " + keyword);
            }
        }

//        Fetch current logged-in profile contract by user , contract status , and From & To date
        Page<Contract> contracts = contractRepository.findMyContracts(currentProfile, contractStatus, startFrom, endTo, pageable);
//        Convert in DTO
        return contracts.map(this::contractDetailsResponseDTO);
    }

//    Contract
    public ContractDetailsResponseDTO getContract(Long contractId){
//        Fetch Contract by ID
        Contract contract = contractRepository.findById(contractId).orElseThrow(() -> new ResourceNotFoundException("Contract not found by ID : " + contractId));
//        Get Current Logged-in Profile
        UserEntity currentProfile = authService.getCurrentProfile();

//        Check Client and GIG authority by contract
        boolean isGig = currentProfile.getGig() != null && contract.getGig().getUser().getId().equals(currentProfile.getId());
        boolean isClient = contract.getClient().getId().equals(currentProfile.getId());

        if (!isClient && !isGig)
            throw new ForbiddenException("You are not a participant of this contract");

        return contractDetailsResponseDTO(contract);
    }

//    Complete contract
    public void completeContract(Long contractId){
//        Get contract by ID
        Contract contract = contractRepository.findById(contractId).orElseThrow(() -> new ResourceNotFoundException("Contract not found by ID : " + contractId));
//        Get current Logged-in Profile
        UserEntity currentProfile = authService.getCurrentProfile();

//        Check Only Client can Complete Contract
        if(!currentProfile.getRoles().contains(Roles.CLIENT))
            throw new ForbiddenException("You are not authorized update contract..");

//        Check -> Contract can't be COMPLETE/WITHDRAWN/CANCEL for marking Complete
        if(contract.getContractStatus().equals(ContractStatus.COMPLETE) || contract.getContractStatus().equals(ContractStatus.WITHDRAWN)  || contract.getContractStatus().equals(ContractStatus.CANCEL))
            throw new InvalidStatusException("Contract is Already "+contract.getContractStatus());

//        Check -> Client Authority
        if(!contract.getClient().getId().equals(currentProfile.getId()))
            throw new ForbiddenException("You are not authorized update contract..");

//        Contract Progress status must be complete for marking Contract status Complete
        if(contract.getProgressStatus().equals(ProgressStatus.NOT_STARTED) || contract.getProgressStatus().equals(ProgressStatus.IN_PROGRESS))
            throw new InvalidStatusException("Contract Work Progress is : "+contract.getProgressStatus().name());

//        Set contract complete and save in DB
        contract.setContractStatus(ContractStatus.COMPLETE);
        contractRepository.save(contract);
//        Sent notification
        notificationService.notify(
                contract.getGig().getUser(),
                NotificationType.CONTRACT_COMPLETED,
                "Contract Completed",
                "The contract for \"" + contract.getJob().getTitle() + "\" has been completed.",
                contract.getId()
        );
    }

//    Active Contract
    public void activeContract(Long contractId){
//        Get Contract by ID
        Contract contract = contractRepository.findById(contractId).orElseThrow(() -> new ResourceNotFoundException("Contract not found by ID : " + contractId));
//        Get Current Logged-in Profile
        UserEntity currentProfile = authService.getCurrentProfile();

//        Only Client can Active Contract
        if(!currentProfile.getRoles().contains(Roles.CLIENT)){
            throw new ForbiddenException("You are not authorized update contract..");
        }

//        Contract must be in Pending
        if(contract.getContractStatus().equals(ContractStatus.COMPLETE) || contract.getContractStatus().equals(ContractStatus.ACTIVE)){
            throw new InvalidStatusException("Contract is : "+contract.getContractStatus()+"..");
        }

        if(contract.getContractStatus().equals(ContractStatus.WITHDRAWN) || contract.getContractStatus().equals(ContractStatus.CANCEL) ){
            throw new InvalidStatusException("Contract is "+contract.getContractStatus()+" by "+contract.getActionInitiatedBy());
        }

//        Check Client Authority
        if(!contract.getClient().getId().equals(currentProfile.getId())){
            throw new ForbiddenException("You are not authorized update contract..");
        }

        if(contract.getProgressStatus().equals(ProgressStatus.IN_PROGRESS) || contract.getProgressStatus().equals(ProgressStatus.COMPLETED)){
            throw new InvalidStatusException("Contract Work is in  : "+contract.getProgressStatus().name());
        }

//        Set Contract Active and save in DB
        contract.setContractStatus(ContractStatus.ACTIVE);
        contractRepository.save(contract);
        notificationService.notify(
                contract.getGig().getUser(),
                NotificationType.CONTRACT_STARTED,
                "Contract Started",
                "The contract for \"" + contract.getJob().getTitle() + "\" has been started.",
                contract.getId()
        );
    }

//    Cancel or Withdrawn Contract
    @Transactional
    public void cancelOrWithdrawnContract(Long contractId,ContractCancelOrWithdrawnRequestDTO dto) {
//        Find Contract
        Contract contract = contractRepository.findById(contractId).orElseThrow(() ->new ResourceNotFoundException("Contract not found by ID : " + contractId));
//        Current Logged-in Profile
        UserEntity currentProfile = authService.getCurrentProfile();

//        Contract already completed
        if (contract.getContractStatus() == ContractStatus.COMPLETE) {
            throw new InvalidStatusException("Contract is already completed");
        }

//        Check actual relationship with contract
        boolean isClient = contract.getClient().getId().equals(currentProfile.getId());
        boolean isGig = contract.getGig().getUser().getId().equals(currentProfile.getId());

//        User is neither Client nor GIG of this contract
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

//        Validate cancellation reason
        ContractCancelReason contractCancelReason;
        try {
            contractCancelReason = ContractCancelReason.valueOf(dto.getReason().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidStatusException("Invalid Cancel reason : " + dto.getReason());
        }

//        Set cancellation details
        contract.setCancelReason(contractCancelReason);
        contract.setCancelledAt(LocalDateTime.now());
        contract.setCancellationRemark(dto.getRemark());

//        Save changes in DB
        contractRepository.save(contract);

        UserEntity recipient = isClient ? contract.getGig().getUser() : contract.getClient();
        String action = contract.getContractStatus() == ContractStatus.WITHDRAWN ? "withdrawn" : "cancelled";

        notificationService.notify(
                recipient,
                NotificationType.CONTRACT_CANCELLED,
                "Contract " + (action.equals("withdrawn") ? "Withdrawn" : "Cancelled"),
                "The contract for \"" + contract.getJob().getTitle() + "\" has been " + action + ".",
                contract.getId()
        );

    }

//    Helper methods
    private ContractDetailsResponseDTO contractDetailsResponseDTO(Contract contract){
        String client = contract.getClient().getFirstName() + " " + contract.getClient().getLastName();
        String gig = contract.getGig().getUser().getFirstName() + " " + contract.getGig().getUser().getLastName();
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