package org.riteshingle.campusgig.Service;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.riteshingle.campusgig.Enum.*;
import org.riteshingle.campusgig.Exception.BadRequestException;
import org.riteshingle.campusgig.Exception.ConflictException;
import org.riteshingle.campusgig.Exception.InvalidStatusException;
import org.riteshingle.campusgig.Exception.ResourceNotFoundException;
import org.riteshingle.campusgig.JwtUtils.JwtUtils;
import org.riteshingle.campusgig.Model.*;
import org.riteshingle.campusgig.Repository.*;
import org.riteshingle.campusgig.RequestDTO.AdminAuthDTO;
import org.riteshingle.campusgig.RequestDTO.AdminSendMailRequestDTO;
import org.riteshingle.campusgig.ResponseDTO.*;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

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
    private final UserSkillsRepository userSkillsRepository;
    private final NotificationService notificationService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AdminRefreshTokenRepository adminRefreshTokenRepository;
    private final AdminRepository adminRepository;

    //    Register User
    public void register(AdminAuthDTO dto)  {
//        Check user is already exists or not ?
        Optional<Admin> byEmail = adminRepository.findByEmail(dto.getEmail());
        if (byEmail.isPresent()){
            throw new ConflictException("Admin already Exists with : " + dto.getEmail());
        }

        Set<Roles> roles = Set.of(Roles.ADMIN);

//        Create User Entity and Save in DB
        Admin admin = Admin.builder()
                .password(passwordEncoder.encode(dto.getPassword()))
                .email(dto.getEmail())
                .roles(roles)
                .adminStatus(AdminStatus.ACTIVE)
                .adminAccessStatus(AdminAccessStatus.PENDING)
                .build();

        adminRepository.save(admin);
    }

    public Map<String, String> login(AdminAuthDTO dto, HttpServletResponse response) {
//        Token Expiry
        Date ACCESS_TOKEN_EXPIRY = new Date(System.currentTimeMillis() + (15 * 60 * 1000));
        Date REFRESH_TOKEN_EXPIRY = new Date(System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000));

//        Get a user by Email
        Admin admin = adminRepository.findByEmail(dto.getEmail()).orElseThrow(() -> new ResourceNotFoundException("User not found"));
//        Get RefreshToken By user
        Optional<AdminRefreshToken> byAdmin = adminRefreshTokenRepository.findByAdmin(admin);
        AdminRefreshToken adminRefreshToken;
        String refresh;

//        If user is present
        if (byAdmin.isPresent()) {
            adminRefreshToken = byAdmin.get();
            boolean tokenExpired;

//            Check is token expire or not
            try {
                tokenExpired = jwtUtils.isExpire(adminRefreshToken.getRefreshToken());
            } catch (Exception e) {
                tokenExpired = true;
            }

//            If is expired then generate new token and save in DB
            if (tokenExpired) {
                refresh = jwtUtils.generateToken(dto.getEmail(), REFRESH_TOKEN_EXPIRY,admin.getRoles());
                adminRefreshToken.setRefreshToken(refresh);
                adminRefreshTokenRepository.save(adminRefreshToken);
            }
//            If token is not expired then get existing one
            else {
                refresh = adminRefreshToken.getRefreshToken();
            }
        }
//        Create a new entity and Generate token and save in DB
        else {
            refresh = jwtUtils.generateToken(dto.getEmail(), REFRESH_TOKEN_EXPIRY,admin.getRoles());
            adminRefreshToken = AdminRefreshToken.builder().refreshToken(refresh).admin(admin).build();
            adminRefreshTokenRepository.save(adminRefreshToken);
        }

//        Set refresh token in cookie
        ResponseCookie cookie = ResponseCookie.from("RefreshToken", refresh)
                .httpOnly(true)
                .secure(false)
                .path("/auth/refresh-token")
                .maxAge(Duration.ofDays(7))
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

//        Generate and Return Access token
        try {
            String accessToken = jwtUtils.generateToken(dto.getEmail(), ACCESS_TOKEN_EXPIRY,admin.getRoles());
            return Map.of("Access Token", accessToken);
        } catch (Exception e) {
            throw new RuntimeException("Invalid Credential");
        }
    }

    public AdminDashboardCardStatsResponseDTO dashboardCardStats(LocalDate from, LocalDate to,
                                                                 String jobApplicationStatus,
                                                                 String jobStatus,String contractStatus,
                                                                 String actionInitiated) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException("From date cannot be after to date");
        }

        if (to != null && to.isAfter(LocalDate.now())) {
            throw new BadRequestException("To date cannot be in the future");
        }

        LocalDateTime startFrom = from == null ? LocalDate.now().atStartOfDay() : from.atStartOfDay();
        LocalDateTime endTo = to == null ? LocalDate.now().atTime(LocalTime.MAX) : to.atTime(LocalTime.MAX);

        JobApplicationStatus applicationStatus;

        try {
            applicationStatus = JobApplicationStatus.valueOf(jobApplicationStatus.trim().toUpperCase());
        }catch (Exception e){
            throw new InvalidStatusException("Invalid Job Application Status : "+jobApplicationStatus);
        }

        JobStatus status;
        try {
            status  = JobStatus.valueOf(jobStatus.trim().toUpperCase());
        }catch (Exception e){
            throw new InvalidStatusException("Invalid Job Status : "+jobStatus);
        }

        ContractStatus cs;
        try {
            cs = ContractStatus.valueOf(contractStatus.trim().toUpperCase());
        }catch (Exception e){
            throw new InvalidStatusException("Invalid Contract Status : "+contractStatus);
        }

        ActionInitiatedBy actionInitiatedBy;
        try {
            actionInitiatedBy = ActionInitiatedBy.valueOf(actionInitiated.trim().toUpperCase());
        }catch (Exception e){
            throw new InvalidStatusException("Invalid Report Status : "+actionInitiated);
        }

        Long totalClient = userEntityRepository.findTotalUserByStatus(Roles.CLIENT, startFrom, endTo);
        Long totalUSER = userEntityRepository.findTotalUserByStatus(Roles.USER, startFrom, endTo);
        Long totalGIG = gigRepository.findTotalGIGByStatus(startFrom, endTo);
        Long totalJobApplication = jobApplicationRepository.findTotalJobApplicationByStatus(applicationStatus, startFrom, endTo);
        Long totalOpenJob = jobRepository.findTotalJobByStatus(status, startFrom, endTo);
        Long totalContract = contractRepository.findTotalContractByStatus(cs, startFrom, endTo);
        Long totalReport = reportRepository.findTotalActionInitiatedBy(actionInitiatedBy, startFrom, endTo);

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
        if (from != null && to != null && from.isAfter(to))
            throw new BadRequestException("From date cannot be after to date");

        if (to != null && to.isAfter(LocalDate.now()))
            throw new BadRequestException("To date cannot be in the future");

        LocalDateTime startFrom = from == null ? LocalDate.now().atStartOfDay() : from.atStartOfDay();
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

    public List<GrowthChartResponseDTO> growthChart(LocalDate from,LocalDate to) {
        if (from != null && to != null && from.isAfter(to))
            throw new BadRequestException("From date cannot be after to date");

        if (to != null && to.isAfter(LocalDate.now()))
            throw new BadRequestException("To date cannot be in the future");

        LocalDateTime localDateTime = LocalDateTime.now();

        LocalDateTime startFrom = from == null ? localDateTime.with(TemporalAdjusters.firstDayOfMonth()).with(LocalTime.MIN) : from.atStartOfDay();

        LocalDateTime endTo = LocalDateTime.now();

        if (startFrom.isAfter(LocalDateTime.now())) {
            throw new RuntimeException("From date cannot be a future date");
        }

        List<Object[]> userGrowth = userEntityRepository.getUserGrowth(Roles.USER, startFrom, endTo);
        List<Object[]> gigGrowth = gigRepository.getGigGrowth(startFrom, endTo);
        List<Object[]> clientGrowth = userEntityRepository.getClientGrowth(Roles.CLIENT, startFrom, endTo);
        List<Object[]> jobGrowth = jobRepository.getJobGrowth(startFrom, endTo);
        List<Object[]> applicationGrowth = jobApplicationRepository.getApplicationGrowth(startFrom,endTo);

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

    public void adminMail(AdminSendMailRequestDTO requestDTO){
        boolean exists = userEntityRepository.existsByEmail(requestDTO.getTo());
        if(!exists) throw new ResourceNotFoundException("User not exists by : "+requestDTO.getTo());
        notificationService.sendMail(requestDTO.getTo(),requestDTO.getSubject(),requestDTO.getBody());
    }

//    Earning Chart/Board
//    Management API's

    //    GIG
    public AdminGigResponseDTO gig(Long gigId) {
        GIG gig = gigRepository.findById(gigId).orElseThrow(() -> new ResourceNotFoundException("GIG not found by ID : " + gigId));
        return adminGigResponseDTO(gig);
    }

    //    GIG list
    public List<AdminGigResponseDTO> gigs(Pageable pageable) {
        return gigRepository.findAll(pageable).stream().map(this::adminGigResponseDTO).toList();
    }

    //    Client
    public AdminUserAndClientResponseDTO client(Long clientId) {
        UserEntity userEntity = userEntityRepository.findById(clientId).orElseThrow(() -> new ResourceNotFoundException("Client not found by ID : "+clientId));
        return userAndClientResponseDTO(userEntity);
    }

    //    Client List
    public List<AdminUserAndClientResponseDTO> clients(Pageable pageable) {
        return userEntityRepository.findAll(pageable).stream().map(this::userAndClientResponseDTO).toList();
    }

    //    Job
    public AdminJobResponseDTO job(Long jobId) {
        Job job = jobRepository.findById(jobId).orElseThrow(() -> new ResourceNotFoundException("Job not found by ID : "+jobId));
        AdminJobResponseDTO adminJobResponseDTO = jobResponseDTO(job);
        adminJobResponseDTO.setClientResponseDTO(userAndClientResponseDTO(job.getClient()));
        return adminJobResponseDTO;
    }

    //    Jobs
    public List<AdminJobResponseDTO> jobs(Pageable pageable) {
        List<Job> jobs = jobRepository.findAll(pageable).getContent();
        return jobs.stream().map(this::jobResponseDTO).toList();
    }

    public AdminJobApplicationResponseDTO jobApplication(Long jobApplicationId) {
        JobApplication jobApplication = jobApplicationRepository.findById(jobApplicationId).orElseThrow(() -> new ResourceNotFoundException("Job Application not found by Job Application ID : "+jobApplicationId));
        return jobApplicationResponseDTO(jobApplication);
    }

    public List<AdminJobApplicationListResponseDTO> jobApplications(Pageable pageable){
        List<JobApplication> content = jobApplicationRepository.findAll(pageable).getContent();
        return content.stream().map(this::jobApplicationListResponseDTO).toList();
    }

    public List<AdminReportListResponseDTO> reports(Pageable pageable){
        List<Report> content = reportRepository.findAll(pageable).getContent();
        return content.stream().map(report -> AdminReportListResponseDTO.builder()
                .reason(report.getReportReason().name())
                .reportedBy(report.getActionInitiatedBy().name())
                .description(report.getDescription())
                .reportStatus(report.getReportStatus().name())
                .createdAt(report.getCreatedAt())
                .id(report.getId())
                .build()
        ).toList();
    }

    public AdminReportResponseDTO report(Long reportId){
        Report report = reportRepository.findById(reportId).orElseThrow(() -> new ResourceNotFoundException("Report not found by ID : "+reportId));
        return reportResponseDTO(report);
    }


    //    Helper method
    private AdminGigResponseDTO adminGigResponseDTO(GIG gig) {
        AdminUserAndClientResponseDTO owner = userAndClientResponseDTO(gig.getUser());
        List<String> skills = userSkillsRepository.findSkillByGigId(gig.getId());

        return AdminGigResponseDTO.builder()
                .id(gig.getId())
                .title(gig.getTitle())
                .availabilityStatus(gig.getAvailabilityStatus())
                .description(gig.getDescription())
                .createdAt(gig.getCreatedAt())
                .category(gig.getTitle())
                .owner(owner)
                .semester(gig.getSemester())
                .department(gig.getDepartment())
                .college(gig.getCollege())
                .skills(skills)
                .build();
    }

    private AdminUserAndClientResponseDTO userAndClientResponseDTO(UserEntity user) {
        return AdminUserAndClientResponseDTO.builder()
                .phoneNumber(user.getPhoneNumber())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .averageRating(user.getAverageRating())
                .profileImage(user.getProfileImage() == null ? null : user.getProfileImage())
                .createdAt(user.getCreatedAt())
                .id(user.getId())
                .isVerified(user.getIsVerified())
                .dob(user.getDob())
                .build();

    }

    private AdminJobResponseDTO jobResponseDTO(Job job) {
        return AdminJobResponseDTO.builder()
                .jobId(job.getId())
                .deadline(job.getDeadline())
                .description(job.getDescription())
                .experience(job.getExperienceLevel().name())
                .jobStatus(job.getJobStatus().name())
                .publishAt(job.getPublishAt())
                .budget(job.getBudget())
                .category(job.getCategory().name())
                .title(job.getTitle())
                .build();
    }

    private AdminJobApplicationResponseDTO jobApplicationResponseDTO(JobApplication jobApplication) {
        return AdminJobApplicationResponseDTO.builder()
                .id(jobApplication.getId())
                .appliedAt(jobApplication.getCreatedAt())
                .status(jobApplication.getJobApplicationStatus().name())
                .coverLetter(jobApplication.getCoverLetter())
                .proposedAmount(jobApplication.getBidAmount())
                .jobResponseDTO(jobResponseDTO(jobApplication.getJob()))
                .gigResponseDTO(gigResponseDTO(jobApplication.getGig()))
                .build();
    }

    private GigResponseDTO gigResponseDTO(GIG gig) {
        return GigResponseDTO.builder()
                .gigFirstName(gig.getUser().getFirstName())
                .gigLastName(gig.getUser().getLastName())
                .gigEmail(gig.getUser().getEmail())
                .gigPhoneNumber(gig.getUser().getPhoneNumber())
                .description(gig.getDescription())
                .title(gig.getTitle())
                .college(gig.getCollege())
                .department(gig.getDepartment())
                .semester(gig.getSemester())
                .availabilityStatus(gig.getAvailabilityStatus())
                .build();
    }

    private AdminReportResponseDTO reportResponseDTO(Report report) {
        return AdminReportResponseDTO.builder()
                .reportStatus(report.getReportStatus().name())
                .adminRemark(report.getAdminRemark() == null ? null : report.getAdminRemark())
                .description(report.getDescription())
                .reason(report.getReportReason().name())
                .actionInitiatedBy(report.getActionInitiatedBy())
                .createdAt(report.getCreatedAt())
                .resolvedAt(report.getResolveAt() == null ? null : report.getResolveAt())
                .contractResponseDTO(contractResponseDTO(report.getContract()))
                .build();

    }

    private AdminContractResponseDTO contractResponseDTO(Contract contract){
        return AdminContractResponseDTO.builder()
                .contractStatus(contract.getContractStatus())
                .expectedDeliveryDate(contract.getExpectedDeliveryDate())
                .createdAt(contract.getCreatedAt())
                .id(contract.getId())
                .progressStatus(contract.getProgressStatus())
                .clientResponseDTO(userAndClientResponseDTO(contract.getClient()))
                .jobApplicationResponseDTO(jobApplicationResponseDTO(contract.getJobApplication()))
                .agreementAmount(contract.getAgreementAmount())
                .build();
    }

    private AdminJobApplicationListResponseDTO jobApplicationListResponseDTO(JobApplication jobApplication){
        return AdminJobApplicationListResponseDTO.builder()
                .applicantName(jobApplication.getGig().getUser().getFirstName()+" "+jobApplication.getGig().getUser().getLastName())
                .clientEmail(jobApplication.getJob().getClient().getEmail())
                .applicantId(jobApplication.getGig().getId())
                .applicantEmail(jobApplication.getGig().getUser().getEmail())
                .clientName(jobApplication.getJob().getClient().getFirstName()+" "+jobApplication.getJob().getClient().getLastName())
                .clientId(jobApplication.getJob().getClient().getId())
                .appliedAt(jobApplication.getCreatedAt())
                .coverLetter(jobApplication.getCoverLetter())
                .proposedAmount(jobApplication.getBidAmount())
                .status(jobApplication.getJobApplicationStatus().name())
                .id(jobApplication.getId())
                .build();

    }
}
