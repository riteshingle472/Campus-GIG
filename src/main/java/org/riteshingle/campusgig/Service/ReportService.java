package org.riteshingle.campusgig.Service;

import lombok.RequiredArgsConstructor;
import org.riteshingle.campusgig.Enum.*;
import org.riteshingle.campusgig.Exception.ForbiddenException;
import org.riteshingle.campusgig.Exception.InvalidStatusException;
import org.riteshingle.campusgig.Exception.ResourceNotFoundException;
import org.riteshingle.campusgig.Model.*;
import org.riteshingle.campusgig.Repository.*;
import org.riteshingle.campusgig.RequestDTO.ReportRequestDTO;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReportService {
    private final ReportRepository reportRepository;
    private final ContractRepository contractRepository;
    private final AuthService authService;

    public void report(ReportRequestDTO dto) {
        UserEntity currentProfile = authService.getCurrentProfile();
        Contract contract = contractRepository.findById(dto.getContractId())
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found..."));

        if(contract.getContractStatus().equals(ContractStatus.ACTIVE) ||
            contract.getContractStatus().equals(ContractStatus.WITHDRAWN) ||
            contract.getContractStatus().equals(ContractStatus.CANCEL) ||
            contract.getContractStatus().equals(ContractStatus.PENDING)){
                throw new InvalidStatusException("Report can only be submitted for active or completed contract");
        }

        GIG gig = contract.getGig();
        UserEntity client = contract.getClient();
        ActionInitiatedBy reportedBy;

        if(client.getId().equals(currentProfile.getId())){
            reportedBy = ActionInitiatedBy.CLIENT;
        }else if(gig.getUser().getId().equals(currentProfile.getId())) {
            reportedBy = ActionInitiatedBy.GIG;
        }else throw new ForbiddenException("You are not a participant of this contract");



        ReportReason reportReason = ReportReason.valueOf(dto.getReportReasonStatus().trim().toUpperCase());

        Report report = Report.builder()
                .gig(gig)
                .client(client)
                .contract(contract)
                .job(contract.getJob())
                .actionInitiatedBy(reportedBy)
                .reportReason(reportReason)
                .reportStatus(ReportStatus.PENDING)
                .description(dto.getDescription())
                .build();

        reportRepository.save(report);
    }

//    private void validateReportRelationship(
//            GIG gig,
//            UserEntity client,
//            Job job,
//            Contract contract) {
//
//        boolean validRelationship = false;
//
//        // Contract relation
//        if (contract != null) {
//            boolean sameGig =
//                    contract.getGig() != null &&
//                            contract.getGig().getId().equals(gig.getId());
//
//            boolean sameClient =
//                    contract.getClient() != null &&
//                            contract.getClient().getId().equals(client.getId());
//
//            validRelationship = sameGig && sameClient;
//        }
//
//        // Job application relation
//        if (!validRelationship && job != null) {
//
//            boolean sameClient =
//                    job.getClient() != null &&
//                            job.getClient().getId().equals(client.getId());
//
//            boolean appliedByGig =
//                    jobApplicationRepository
//                            .existsByJobIdAndGigId(
//                                    job.getId(),
//                                    gig.getId()
//                            );
//
//            validRelationship = sameClient && appliedByGig;
//        }
//
//        if (!validRelationship) {
//            throw new ForbiddenException(
//                    "You cannot report this user because there is no valid relationship"
//            );
//        }
//    }
}
