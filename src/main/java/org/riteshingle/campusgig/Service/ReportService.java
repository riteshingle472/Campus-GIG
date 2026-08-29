package org.riteshingle.campusgig.Service;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.riteshingle.campusgig.Enum.ContractStatus;
import org.riteshingle.campusgig.Enum.ReportReason;
import org.riteshingle.campusgig.Enum.ReportStatus;
import org.riteshingle.campusgig.Enum.ReportedBy;
import org.riteshingle.campusgig.Model.Contract;
import org.riteshingle.campusgig.Model.GIG;
import org.riteshingle.campusgig.Model.Report;
import org.riteshingle.campusgig.Model.UserEntity;
import org.riteshingle.campusgig.Repository.ContractRepository;
import org.riteshingle.campusgig.Repository.GigRepository;
import org.riteshingle.campusgig.Repository.ReportRepository;
import org.riteshingle.campusgig.Repository.UserEntityRepository;
import org.riteshingle.campusgig.RequestDTO.ReportRequestDTO;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReportService {
    private final ReportRepository reportRepository;
    private final ContractRepository contractRepository;
    private final GigRepository gigRepository;
    private final UserEntityRepository userEntityRepository;
    private final AuthService authService;

    public void report(ReportRequestDTO dto) {
        UserEntity currentProfile = authService.getCurrentProfile();
        Contract contract = contractRepository.findById(dto.getContractId()).orElseThrow(() -> new RuntimeException("Contract not found..."));

        if(contract.getContractStatus().equals(ContractStatus.ACTIVE) ||
            contract.getContractStatus().equals(ContractStatus.CANCEL) ||
            contract.getContractStatus().equals(ContractStatus.WITHDRAWN) ||
            contract.getContractStatus().equals(ContractStatus.PENDING)){
                throw new RuntimeException("Report can only be submitted for active or completed contract");
        }

        GIG gig = contract.getGig();
        UserEntity client = contract.getClient();
        ReportedBy reportedBy;

        if(client.getId().equals(currentProfile.getId())){
            reportedBy = ReportedBy.CLIENT;
        }else if(gig.getUser().getId().equals(currentProfile.getId())) {
            reportedBy = ReportedBy.GIG;
        }else throw new RuntimeException("You are not a participant of this contract");

        if (reportRepository.existsByContractIdAndReportedBy(dto.getContractId(), reportedBy))
            throw new RuntimeException("You have already submitted a report for this contract");

        ReportReason reportReason = ReportReason.valueOf(dto.getReportReasonStatus().trim().toUpperCase());

        Report report = Report.builder()
                .gig(gig)
                .client(client)
                .contract(contract)
                .job(contract.getJob())
                .reportedBy(reportedBy)
                .reportReason(reportReason)
                .reportStatus(ReportStatus.PENDING)
                .description(dto.getDescription())
                .build();

        reportRepository.save(report);
    }
}
