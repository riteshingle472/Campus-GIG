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
import org.riteshingle.campusgig.ResponseDTO.ReportResponseDTO;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ReportService {
    private final ReportRepository reportRepository;
    private final ContractRepository contractRepository;
    private final AuthService authService;

//    Report
    public void report(ReportRequestDTO dto) {
        UserEntity currentProfile = authService.getCurrentProfile();
        Contract contract = contractRepository.findById(dto.getContractId())
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found..."));

//        Check Contract must ACTIVE/COMPLETE
        if (!contract.getContractStatus().equals(ContractStatus.ACTIVE) &&
                !contract.getContractStatus().equals(ContractStatus.COMPLETE)) {
            throw new InvalidStatusException("Report can only be submitted for an active or completed contract");
        }

        GIG gig = contract.getGig();
        UserEntity client = contract.getClient();
        ActionInitiatedBy reportedBy;

        if(client.getId().equals(currentProfile.getId())){
            reportedBy = ActionInitiatedBy.CLIENT;
        }else if(gig.getUser().getId().equals(currentProfile.getId())) {
            reportedBy = ActionInitiatedBy.GIG;
        }else throw new ForbiddenException("You are not a participant of this contract");

//        Validate Report Reason
        ReportReason reportReason;
        try {
            reportReason = ReportReason.valueOf(dto.getReportReasonStatus().trim().toUpperCase());
        } catch (Exception e) {
            throw new InvalidStatusException("Invalid report reason: " + dto.getReportReasonStatus());
        }

//        Create report and save in DB
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

//    Reports
    public List<ReportResponseDTO> getMyReports(Pageable pageable) {
//        Get Current Logged-in Profile
        UserEntity currentProfile = authService.getCurrentProfile();
//        Fetch all report by current profile
        List<Report> reports = reportRepository.findMyReports(currentProfile, pageable);

        return reports.stream().map(report -> {
            boolean isClient = report.getClient().getId().equals(currentProfile.getId());
            boolean filedByMe = (isClient && report.getActionInitiatedBy() == ActionInitiatedBy.CLIENT)
                    || (!isClient && report.getActionInitiatedBy() == ActionInitiatedBy.GIG);

            String otherPartyName = isClient
                    ? report.getGig().getUser().getFirstName() + " " + report.getGig().getUser().getLastName()
                    : report.getClient().getFirstName() + " " + report.getClient().getLastName();

            return ReportResponseDTO.builder()
                    .id(report.getId())
                    .jobTitle(report.getJob() != null ? report.getJob().getTitle() : null)
                    .otherPartyName(otherPartyName)
                    .reportReason(report.getReportReason().name())
                    .reportStatus(report.getReportStatus().name())
                    .description(report.getDescription())
                    .filedByMe(filedByMe)
                    .createdAt(report.getCreatedAt())
                    .build();
        }).toList();
    }
}