package org.riteshingle.campusgig.Service;

import lombok.RequiredArgsConstructor;
import org.riteshingle.campusgig.Enum.*;
import org.riteshingle.campusgig.Model.Report;
import org.riteshingle.campusgig.Repository.*;
import org.riteshingle.campusgig.ResponseDTO.AdminDashboardCardStatsResponseDTO;
import org.riteshingle.campusgig.ResponseDTO.AdminDashboardMostPopularJobResponseDTO;
import org.riteshingle.campusgig.ResponseDTO.GrowthChartResponseDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminService {
    private final UserEntityRepository userEntityRepository;
    private final GigRepository gigRepository;
    private final JobRepository jobRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final ContractRepository contractRepository;
    private final ReportRepository reportRepository;

    public AdminDashboardCardStatsResponseDTO dashboardCardStats(LocalDate from,LocalDate to) {
        LocalDateTime startFrom = from == null ? LocalDateTime.now().with(TemporalAdjusters.firstDayOfMonth()) : from.atStartOfDay();
        LocalDateTime endTo = to == null ? LocalDateTime.now() : to.atTime(LocalTime.MAX);

        LocalDate start = from == null ? LocalDate.now().with(TemporalAdjusters.firstDayOfMonth()) : from;
        LocalDate end = to == null ? LocalDate.now().with(TemporalAdjusters.lastDayOfMonth()) : to;

        Long totalClient = userEntityRepository.findTotalUserByStatus(Roles.CLIENT,startFrom,endTo);
        Long totalUSER = userEntityRepository.findTotalUserByStatus(Roles.USER,startFrom,endTo);
        Long totalGIG = gigRepository.findTotalGIGByStatus(startFrom,endTo);
        Long totalJobApplication = jobApplicationRepository.findTotalJobApplicationByStatus(JobApplicationStatus.APPLIED,start,end);
        Long totalOpenJob = jobRepository.findTotalJobByStatus(JobStatus.OPEN,startFrom,endTo);
        Long totalContract = contractRepository.findTotalContractByStatus(ContractStatus.ACTIVE,startFrom,endTo);
        Long totalReport = reportRepository.findTotalReportByStatus(ReportStatus.PENDING, startFrom, endTo);

        return AdminDashboardCardStatsResponseDTO.builder()
                .totalJob(totalOpenJob)
                .totalGIG(totalGIG)
                .totalClient(totalClient)
                .totalContract(totalContract)
                .totalJobApplication(totalJobApplication)
                .totalUser(totalUSER)
                .totalReport(totalReport)
                .build();
    }

    public List<AdminDashboardMostPopularJobResponseDTO> mostPopularJob(LocalDate from, LocalDate to) {
        LocalDateTime startFrom = from == null ? LocalDateTime.now().with(TemporalAdjusters.firstDayOfMonth()) : from.atStartOfDay();
        LocalDateTime endTo = to == null ? LocalDateTime.now() : to.atTime(LocalTime.MAX);

        if (startFrom.isAfter(LocalDateTime.now())) {
            throw new RuntimeException("From date cannot be a future date");
        }

        if (endTo.isAfter(LocalDateTime.now().plusDays(1))) {
            throw new RuntimeException("To date cannot be a future date");
        }

        List<Object[]> popularJob = jobRepository.findPopularJobCategories(startFrom, endTo);
        return popularJob.stream().map(job -> new AdminDashboardMostPopularJobResponseDTO(
                (Long) job[1],
                (JobCategory) job[0]
        )).toList();
    }

    public List<GrowthChartResponseDTO> growthChart(LocalDate from) {
        LocalDateTime localDateTime = LocalDateTime.now();

        LocalDateTime startFrom = from == null ? localDateTime.with(TemporalAdjusters.firstDayOfMonth()).with(LocalTime.MIN) : from.atStartOfDay();

        LocalDateTime endTo = LocalDateTime.now();

        LocalDate start = from == null ? LocalDate.now().with(TemporalAdjusters.firstDayOfMonth()).with(LocalDate.MIN) : from.with(TemporalAdjusters.firstDayOfMonth());

        LocalDate end = LocalDate.now();


        if (startFrom.isAfter(LocalDateTime.now())) {
            throw new RuntimeException("From date cannot be a future date");
        }

        List<Object[]> userGrowth = userEntityRepository.getUserGrowth(Roles.USER ,startFrom, endTo);
        List<Object[]> gigGrowth = gigRepository.getGigGrowth(startFrom, endTo);
        List<Object[]> clientGrowth = userEntityRepository.getClientGrowth(Roles.CLIENT, startFrom, endTo);
        List<Object[]> jobGrowth = jobRepository.getJobGrowth(startFrom, endTo);
        List<Object[]> applicationGrowth = jobApplicationRepository.getApplicationGrowth(start, end,JobApplicationStatus.ACCEPTED);

        Map<LocalDate, GrowthChartResponseDTO> growthMap = new TreeMap<>();

        for (Object[] row : userGrowth) {
            LocalDate date = ((java.sql.Date) row[0]).toLocalDate();
            Long count = (Long) row[1];

            GrowthChartResponseDTO dto = growthMap.computeIfAbsent(date,
                    d -> GrowthChartResponseDTO.builder()
                            .date(d)
                            .users(0L)
                            .gigs(0L)
                            .clients(0L)
                            .jobs(0L)
                            .applications(0L)
                            .build()
            );
            dto.setUsers(count);
        }

        for (Object[] row : gigGrowth) {
            LocalDate date = ((java.sql.Date) row[0]).toLocalDate();
            Long count = (Long) row[1];

            GrowthChartResponseDTO dto = growthMap.computeIfAbsent(date,
                    d -> GrowthChartResponseDTO.builder()
                            .date(d)
                            .users(0L)
                            .gigs(0L)
                            .clients(0L)
                            .jobs(0L)
                            .applications(0L)
                            .build()
            );
            dto.setGigs(count);
        }
        ;

        for (Object[] row : clientGrowth) {

            LocalDate date = ((java.sql.Date) row[0]).toLocalDate();
            Long count = (Long) row[1];

            GrowthChartResponseDTO dto = growthMap.computeIfAbsent(date,
                    d -> GrowthChartResponseDTO.builder()
                            .date(d)
                            .users(0L)
                            .gigs(0L)
                            .clients(0L)
                            .jobs(0L)
                            .applications(0L)
                            .build()
            );

            dto.setClients(count);
        }

        for (Object[] row : applicationGrowth) {

            LocalDate date = ((java.sql.Date) row[0]).toLocalDate();
            Long count = (Long) row[1];

            GrowthChartResponseDTO dto = growthMap.computeIfAbsent(date,
                    d -> GrowthChartResponseDTO.builder()
                            .date(d)
                            .users(0L)
                            .gigs(0L)
                            .clients(0L)
                            .jobs(0L)
                            .applications(0L)
                            .build()
            );

            dto.setApplications(count);
        }

        for (Object[] row : jobGrowth) {

            LocalDate date = ((java.sql.Date) row[0]).toLocalDate();
            Long count = (Long) row[1];

            GrowthChartResponseDTO dto = growthMap.computeIfAbsent(date,
                    d -> GrowthChartResponseDTO.builder()
                            .date(d)
                            .users(0L)
                            .gigs(0L)
                            .clients(0L)
                            .jobs(0L)
                            .applications(0L)
                            .build()
            );

            dto.setJobs(count);
        }

        return new ArrayList<>(growthMap.values());
    }

//    Earning Chart/Board

}
