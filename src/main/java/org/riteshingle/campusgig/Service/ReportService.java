package org.riteshingle.campusgig.Service;

import lombok.RequiredArgsConstructor;
import org.riteshingle.campusgig.Enum.*;
import org.riteshingle.campusgig.Exception.ForbiddenException;
import org.riteshingle.campusgig.Exception.InvalidStatusException;
import org.riteshingle.campusgig.Exception.ResourceNotFoundException;
import org.riteshingle.campusgig.Model.*;
import org.riteshingle.campusgig.Repository.*;
import org.riteshingle.campusgig.RequestDTO.ReportRequestDTO;
import org.riteshingle.campusgig.ResponseDTO.ReportResponseDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    @Transactional
    public List<ReportResponseDTO> reports(){
//        Get Current Logged-in Profile
        UserEntity currentProfile = authService.getCurrentProfile();
        Roles roles = currentProfile.getRoles().iterator().next();

        if(!currentProfile.getIsVerified()){
            throw new IllegalArgumentException("Profile is not verified");
        }

        List<Report> reports;
        if(roles.equals(Roles.GIG)){
            reports = reportRepository.findReports(ActionInitiatedBy.GIG);
        }else {
            reports = reportRepository.findReports(ActionInitiatedBy.CLIENT);
        }

        return reports.stream()
                .map(report -> ReportResponseDTO.builder()
                        .id(report.getId())
                        .reason(report.getReportReason().name())
                        .actionInitiatedBy(report.getActionInitiatedBy())
                        .description(report.getDescription())
                        .reportStatus(report.getReportStatus().name())
                        .adminRemark(report.getAdminRemark())
                        .createdAt(report.getCreatedAt())
                        .resolvedAt(report.getResolveAt())
                        .build()
                )
                .toList();
    }
}
