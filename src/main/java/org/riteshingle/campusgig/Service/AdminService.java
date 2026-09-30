package org.riteshingle.campusgig.Service;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.riteshingle.campusgig.AdminResponseDTO.*;
import org.riteshingle.campusgig.Enum.*;
import org.riteshingle.campusgig.Exception.*;
import org.riteshingle.campusgig.JwtUtils.JwtUtils;
import org.riteshingle.campusgig.Model.*;
import org.riteshingle.campusgig.Repository.*;
import org.riteshingle.campusgig.AdminRequestDTO.AdminAuthDTO;
import org.riteshingle.campusgig.ResponseDTO.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final JwtUtils jwtUtils;
    private final GigRepository gigRepository;
    private final JobRepository jobRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminRepository adminRepository;
    private final ReportRepository reportRepository;
    private final ContractRepository contractRepository;
    private final UserEntityRepository userEntityRepository;
    private final UserSkillsRepository userSkillsRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final TechnicalSupportRepository technicalSupportRepository;
    private final AdminRefreshTokenRepository adminRefreshTokenRepository;

    @Resource(name = "adminAuthenticationManager")
    private final AuthenticationManager adminAuthenticationManager;

    @Value("${main.admin.email}")
    private String mainAdminEmail;

//    Register Admin
    public void register(AdminAuthDTO dto)  {
//        Check Email is already present or not in DB
        Optional<Admin> byEmail = adminRepository.findByEmail(dto.getEmail());

//        Throw Conflict Exception if admin already present
        if (byEmail.isPresent())
            throw new ConflictException("Admin already Exists with : " + dto.getEmail());

//        Set Role Admin
        Set<Roles> roles = Set.of(Roles.ADMIN);

//        Creating Admin and save in DB
        Admin admin = Admin.builder()
                .password(passwordEncoder.encode(dto.getPassword()))
                .email(dto.getEmail())
                .fullName(dto.getFullName())
                .contactNo(dto.getContactNo())
                .roles(roles)
                .adminStatus(AdminStatus.ACTIVE)
                .adminAccessStatus(AdminAccessStatus.PENDING)
                .build();

        if(mainAdminEmail.equals(admin.getEmail())){
            admin.setAdminAccessStatus(AdminAccessStatus.ALLOWED);
        }

        adminRepository.save(admin);
    }

//    Admin Login
    public Map<String, String> login(AdminAuthDTO dto, HttpServletResponse response) {
//        Token Expiry
        Date REFRESH_TOKEN_EXPIRY = new Date(System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000));
        Date ACCESS_TOKEN_EXPIRY = new Date(System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000));

//        Get Admin By email
        Admin admin = adminRepository.findByEmail(dto.getEmail()).orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (admin.getAdminAccessStatus() == AdminAccessStatus.PENDING)
            throw new UnauthorizedException("Your admin access is pending approval");

        if (admin.getAdminAccessStatus() == AdminAccessStatus.DENIED)
            throw new UnauthorizedException("Your admin access has been denied");

//        Authentication Manager for matching password
        try {adminAuthenticationManager.authenticate(new UsernamePasswordAuthenticationToken(dto.getEmail(),dto.getPassword()));}
        catch (Exception e) {
            e.printStackTrace();
            throw e;
        }

//        Check admin refresh token in DB by Admin
        Optional<AdminRefreshToken> byAdmin = adminRefreshTokenRepository.findByAdmin(admin);
        AdminRefreshToken adminRefreshToken;
        String refresh;

        if (byAdmin.isPresent()) {
//            Get existing Refresh Token
            adminRefreshToken = byAdmin.get();
            boolean tokenExpired;

//            Check token is Expired or not
            try {tokenExpired = jwtUtils.isExpire(adminRefreshToken.getRefreshToken());}
            catch (Exception e) {tokenExpired = true;}

//            If token is expired then create new token and save in DB
            if (tokenExpired) {
                refresh = jwtUtils.generateToken(dto.getEmail(), REFRESH_TOKEN_EXPIRY, admin.getRoles());
                adminRefreshToken.setRefreshToken(refresh);
                adminRefreshTokenRepository.save(adminRefreshToken);
            } else {
//                If token is not expired then getting existing token
                refresh = adminRefreshToken.getRefreshToken();
            }
        }
//        Creating new AdminRefreshToken and saving in DB if RefreshToken isn't present
        else {
            refresh = jwtUtils.generateToken(dto.getEmail(), REFRESH_TOKEN_EXPIRY, admin.getRoles());
            adminRefreshToken = AdminRefreshToken.builder().refreshToken(refresh).admin(admin).build();
            adminRefreshTokenRepository.save(adminRefreshToken);
        }

//        Set Refresh Token in Cookies for 7 Days
        ResponseCookie cookie = ResponseCookie.from("RefreshToken", refresh)
                .httpOnly(true)
                .secure(false)
                .path("/api/auth/refresh-token")
                .maxAge(Duration.ofDays(7))
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

//        Generating new Access Token
        String accessToken = jwtUtils.generateToken(dto.getEmail(), ACCESS_TOKEN_EXPIRY, admin.getRoles());
        return Map.of("Access Token", accessToken);
    }

    public Admin getCurrentAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return adminRepository.findByEmailWithRoles(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

//    Admins
    public List<AdminResponseDTO> admins(Pageable pageable,String keyword){
//        Get current logged-in Admin
        Admin currentAdmin = getCurrentAdmin();

//        Only admin can see  all admins
        if(!currentAdmin.getEmail().equals(mainAdminEmail))
            throw new ForbiddenException("You don't have permission to view Admins");

//        Validate Admin
        AdminAccessStatus adminAccessStatus = null;

        if(keyword != null){
            try {adminAccessStatus = AdminAccessStatus.valueOf(keyword.trim().toUpperCase());}
            catch(InvalidStatusException exception){ throw new InvalidStatusException("Invalid status : "+keyword.trim().toUpperCase());}
        }
//        Fetch all admin by keyword (ALLOWED,PENDING,DENIED)
        List<Admin> admins = adminRepository.findAdminByKeywords(adminAccessStatus, pageable);
//        convert and return Admin list into AdminResponseDTO list
        return admins.stream().map(this::adminResponseDTO).toList();
    }

//    Approve Admin
    public void adminAccess(String status,Long id){
//        Get current logged-in Admin
        Admin currentAdmin = getCurrentAdmin();

//        Only Main admin can give permission to other admin to logg-in
        if(!currentAdmin.getEmail().equals(mainAdminEmail))
            throw new UnauthorizedException("You don't have permission to that..");

//        Fetch admin by id
        Admin admin = adminRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Admin not found by Id : " + id));

//        Main admin can't set access status on own
        if(currentAdmin.getId().equals(admin.getId()))
            throw new BadRequestException("You can't perform this action on yourself");

//        Validate admin access status
        AdminAccessStatus adminAccessStatus;
        try {adminAccessStatus = AdminAccessStatus.valueOf(status.trim().toUpperCase());}
        catch (InvalidStatusException exception){throw new InvalidStatusException("Invalid Admin Status : "+status.trim().toUpperCase());}

//        Admin can't set status to pending
        if(adminAccessStatus == AdminAccessStatus.PENDING)
            throw new BadRequestException("Invalid Status Selection : "+adminAccessStatus);

//        set and save changes
        admin.setAdminAccessStatus(adminAccessStatus);
        adminRepository.save(admin);
    }

//    Dashboard Stats
    public AdminDashboardCardStatsResponseDTO dashboardCardStats(LocalDate from, LocalDate to,
                                                                 String jobApplicationStatus,
                                                                 String jobStatus, String contractStatus,
                                                                 String reportStatus) {

//        From date cannot be after to date
        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException("From date cannot be after to date");
        }

//        to date can't be future date
        if (to != null && to.isAfter(LocalDate.now())) {
            throw new BadRequestException("To date cannot be in the future");
        }

//        Set From and To date
        LocalDateTime startFrom = from == null ? LocalDate.now().atStartOfDay() : from.atStartOfDay();
        LocalDateTime endTo = to == null ? LocalDate.now().atTime(LocalTime.MAX) : to.atTime(LocalTime.MAX);

//        Validate and get Job Application Status
        JobApplicationStatus applicationStatus;
        try {
            applicationStatus = JobApplicationStatus.valueOf(jobApplicationStatus.trim().toUpperCase());
        }catch (Exception e){
            throw new InvalidStatusException("Invalid Job Application Status : "+jobApplicationStatus);
        }

//        Validate and get Job Status
        JobStatus status;
        try {
            status  = JobStatus.valueOf(jobStatus.trim().toUpperCase());
        }catch (Exception e){
            throw new InvalidStatusException("Invalid Job Status : "+jobStatus);
        }

//        Validate and get Contract Status
        ContractStatus cs;
        try {
            cs = ContractStatus.valueOf(contractStatus.trim().toUpperCase());
        }catch (Exception e){
            throw new InvalidStatusException("Invalid Contract Status : "+contractStatus);
        }

//        Validate and get Report Status
        ReportStatus adminReportStatus;
        try {
            adminReportStatus = ReportStatus.valueOf(reportStatus.trim().toUpperCase());
        }catch (Exception e){
            throw new InvalidStatusException("Invalid Report Status : "+reportStatus);
        }

//        Get Total Client , User , GIG , Job Application , Job , Contract and Reports
        Long totalClient = userEntityRepository.findTotalUserByStatus(Roles.CLIENT, startFrom, endTo);
        Long totalUSER = userEntityRepository.findTotalUserByStatus(Roles.USER, startFrom, endTo);
        Long totalGIG = gigRepository.findTotalGIGByStatus(startFrom, endTo);
        Long totalJobApplication = jobApplicationRepository.findTotalJobApplicationByStatus(applicationStatus, startFrom, endTo);
        Long totalOpenJob = jobRepository.findTotalJobByStatus(status, startFrom, endTo);
        Long totalContract = contractRepository.findTotalContractByStatus(cs, startFrom, endTo);
        Long totalReport = reportRepository.findTotalReportByStatus(adminReportStatus, startFrom, endTo);
        Long totalTechnicalIssue = technicalSupportRepository.findTotalTechnicalIssues(startFrom,endTo);

        return AdminDashboardCardStatsResponseDTO.builder()
                .totalGIG(totalGIG)
                .totalUser(totalUSER)
                .totalJob(totalOpenJob)
                .totalReport(totalReport)
                .totalClient(totalClient)
                .totalContract(totalContract)
                .totalTechnicalIssue(totalTechnicalIssue)
                .totalJobApplication(totalJobApplication)
                .build();
    }

//    Most Popular Job
    public List<AdminDashboardMostPopularJobResponseDTO> mostPopularJob(LocalDate from, LocalDate to) {

//        From date can't be after then To date
        if (from != null && to != null && from.isAfter(to))
            throw new BadRequestException("From date cannot be after to date");

//        To date can't in future
        if (to != null && to.isAfter(LocalDate.now()))
            throw new BadRequestException("To date cannot be in the future");

//        Set From and To date
        LocalDateTime startFrom = from == null ? LocalDate.now().atStartOfDay() : from.atStartOfDay();
        LocalDateTime endTo = to == null ? LocalDateTime.now() : to.atTime(LocalTime.MAX);

//        startFrom can't in future
        if (startFrom.isAfter(LocalDateTime.now()))
            throw new BadRequestException("From date cannot be a future date");

//        endTo can't in future
        if (endTo.isAfter(LocalDateTime.now().plusDays(1)))
            throw new BadRequestException("To date cannot be a future date");

//        Fetching Most Popular Jobs
//        Getting Most Popular Jobs in the form of Object Array
        List<Object[]> popularJob = jobRepository.findPopularJobCategories(startFrom, endTo);
//        Converting in Response DTO
        return popularJob.stream().map(job -> new AdminDashboardMostPopularJobResponseDTO(
                (Long) job[1],
                (JobCategory) job[0]
        )).toList();
    }

    //    CHANGED: RuntimeException -> BadRequestException
    public List<GrowthChartResponseDTO> growthChart(LocalDate from,LocalDate to) {
        if (from != null && to != null && from.isAfter(to))
            throw new BadRequestException("From date cannot be after to date");

        if (to != null && to.isAfter(LocalDate.now()))
            throw new BadRequestException("To date cannot be in the future");

        LocalDateTime localDateTime = LocalDateTime.now();

        LocalDateTime startFrom = from == null ? localDateTime.with(TemporalAdjusters.firstDayOfMonth()).with(LocalTime.MIN) : from.atStartOfDay();

        LocalDateTime endTo = LocalDateTime.now();

        if (startFrom.isAfter(LocalDateTime.now())) {
            throw new BadRequestException("From date cannot be a future date");
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
                    d -> GrowthChartResponseDTO.builder().date(d).users(0L).gigs(0L).clients(0L).jobs(0L).applications(0L).build());
            dto.setUsers(count);
        }

        for (Object[] row : gigGrowth) {
            LocalDate date = ((java.sql.Date) row[0]).toLocalDate();
            Long count = (Long) row[1];
            GrowthChartResponseDTO dto = growthMap.computeIfAbsent(date,
                    d -> GrowthChartResponseDTO.builder().date(d).users(0L).gigs(0L).clients(0L).jobs(0L).applications(0L).build());
            dto.setGigs(count);
        }

        for (Object[] row : clientGrowth) {
            LocalDate date = ((java.sql.Date) row[0]).toLocalDate();
            Long count = (Long) row[1];
            GrowthChartResponseDTO dto = growthMap.computeIfAbsent(date,
                    d -> GrowthChartResponseDTO.builder().date(d).users(0L).gigs(0L).clients(0L).jobs(0L).applications(0L).build());
            dto.setClients(count);
        }

        for (Object[] row : applicationGrowth) {
            LocalDate date = ((java.sql.Date) row[0]).toLocalDate();
            Long count = (Long) row[1];
            GrowthChartResponseDTO dto = growthMap.computeIfAbsent(date,
                    d -> GrowthChartResponseDTO.builder().date(d).users(0L).gigs(0L).clients(0L).jobs(0L).applications(0L).build());
            dto.setApplications(count);
        }

        for (Object[] row : jobGrowth) {
            LocalDate date = ((java.sql.Date) row[0]).toLocalDate();
            Long count = (Long) row[1];
            GrowthChartResponseDTO dto = growthMap.computeIfAbsent(date,
                    d -> GrowthChartResponseDTO.builder().date(d).users(0L).gigs(0L).clients(0L).jobs(0L).applications(0L).build());
            dto.setJobs(count);
        }

        return new ArrayList<>(growthMap.values());
    }

//    public void adminMail(AdminSendMailRequestDTO requestDTO){
//        boolean exists = userEntityRepository.existsByEmail(requestDTO.getTo());
//        if(!exists) throw new ResourceNotFoundException("User not exists by : "+requestDTO.getTo());
//        notificationService.sendMail(requestDTO.getTo(),requestDTO.getSubject(),requestDTO.getBody());
//    }

//    Get Gig by ID
    public AdminGigResponseDTO gig(Long gigId) {
        GIG gig = gigRepository.findById(gigId).orElseThrow(() -> new ResourceNotFoundException("GIG not found by ID : " + gigId));
        return adminGigResponseDTO(gig);
    }

//    Fetch All GIGs
    public List<AdminGigResponseDTO> gigs(Pageable pageable) {
        return gigRepository.findAll(pageable).stream().map(this::adminGigResponseDTO).toList();
    }

//    Get Client by ID
    public AdminUserAndClientResponseDTO client(Long clientId) {
        UserEntity userEntity = userEntityRepository.findById(clientId).orElseThrow(() -> new ResourceNotFoundException("Client not found by ID : "+clientId));
        return userAndClientResponseDTO(userEntity);
    }

//    Fetch All Clients
    public List<AdminUserAndClientResponseDTO> clients(Pageable pageable) {
        return userEntityRepository.findAll(pageable).stream().map(this::userAndClientResponseDTO).toList();
    }

//    Get Job by ID
    public AdminJobResponseDTO job(Long jobId) {
//        Get Job By ID
        Job job = jobRepository.findById(jobId).orElseThrow(() -> new ResourceNotFoundException("Job not found by ID : "+jobId));
//        Convert Job in AdminJobResponseDTO
        AdminJobResponseDTO adminJobResponseDTO = jobResponseDTO(job);
//        Set Job Client
        adminJobResponseDTO.setClientResponseDTO(userAndClientResponseDTO(job.getClient()));
//        Return Response
        return adminJobResponseDTO;
    }

//    Fetch All Jobs
    public List<AdminJobResponseDTO> jobs(Pageable pageable) {
        List<Job> jobs = jobRepository.findAll(pageable).getContent();
//        Convert into JobResponseDTO and Return
        return jobs.stream().map(this::jobResponseDTO).toList();
    }

//    Get Job Application by ID
    public AdminJobApplicationResponseDTO jobApplication(Long jobApplicationId) {
        JobApplication jobApplication = jobApplicationRepository.findById(jobApplicationId).orElseThrow(() -> new ResourceNotFoundException("Job Application not found by Job Application ID : "+jobApplicationId));
        return jobApplicationResponseDTO(jobApplication);
    }

//    Fetch All Job Application
    public List<AdminJobApplicationListResponseDTO> jobApplications(Pageable pageable){
        List<JobApplication> content = jobApplicationRepository.findAll(pageable).getContent();
        return content.stream().map(this::jobApplicationListResponseDTO).toList();
    }

//    Fetch All Reports
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

//    Get Report By ID
    public AdminReportResponseDTO report(Long reportId){
        Report report = reportRepository.findById(reportId).orElseThrow(() -> new ResourceNotFoundException("Report not found by ID : "+reportId));
        return reportResponseDTO(report);
    }

//    Update Report
    public void updateReport(String status,Long id){
//        Fetch report by ID
        Report report = reportRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Report not found by ID : "+id));

//        Validate report status
        ReportStatus reportStatus;
        try {reportStatus = ReportStatus.valueOf(status.trim().toUpperCase());}
        catch (InvalidStatusException exception){throw new InvalidStatusException("Invalid status selection : "+status.trim().toUpperCase());}

//        Validate Report
        validateReportStatusUpdate(report.getReportStatus(),reportStatus);

//        set and save changes
        report.setReportStatus(reportStatus);
        reportRepository.save(report);
    }

    private void validateReportStatusUpdate(
            ReportStatus currentStatus,
            ReportStatus newStatus) {

        if (currentStatus == ReportStatus.RESOLVED || currentStatus == ReportStatus.REJECTED)
            throw new BadRequestException("Report status cannot be updated once it is resolved or rejected");

        if (currentStatus == newStatus)
            throw new BadRequestException("Report is already in " + currentStatus);

        if (currentStatus == ReportStatus.PENDING && newStatus != ReportStatus.UNDER_REVIEW)
            throw new BadRequestException("Pending report must first be moved to UNDER_REVIEW");
    }

//    Get Technical Issue by ID
    public AdminTechnicalSupportResponseDTO technicalSupport(Long id){
        TechnicalSupport technicalSupport = technicalSupportRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Technical Issue not found.."));
        return technicalSupportResponse(technicalSupport);
    }

//    Get Technical Issues
    public List<AdminTechnicalSupportResponseDTO> technicalSupports(Pageable pageable){
        List<TechnicalSupport> content = technicalSupportRepository.findAll(pageable).getContent();
        return content.stream().map(this::technicalSupportResponse).toList();
    }

    public void updateTechnicalIssue(Long id ,String status){
//        Fetch Technical Support by ID
        TechnicalSupport technicalSupport = technicalSupportRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Technical Support not found ..."));

//        Validate Support Status
        TechnicalSupportStatus supportStatus;
        try {supportStatus = TechnicalSupportStatus.valueOf(status.trim().toUpperCase());}
        catch (InvalidStatusException exception){throw new InvalidStatusException("Status is not Valid..."+status);}

//        status transition validation
        validateTechnicalSupportStatus(supportStatus,technicalSupport.getStatus());
//        save changes in DB
        technicalSupport.setStatus(supportStatus);
        technicalSupportRepository.save(technicalSupport);
    }

    //    Helper methods (unchanged)
    private AdminGigResponseDTO adminGigResponseDTO(GIG gig) {
        AdminUserAndClientResponseDTO owner = userAndClientResponseDTO(gig.getUser());
        List<String> skills = userSkillsRepository.findSkillByGigId(gig.getId());

        return AdminGigResponseDTO.builder()
                .id(gig.getId())
                .title(gig.getTitle())
                .availabilityStatus(gig.getAvailabilityStatus())
                .description(gig.getDescription())
                .createdAt(gig.getCreatedAt())
                .category(gig.getJobCategory().name())
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
                .clientResponseDTO(userAndClientResponseDTO(job.getClient()))
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
                .gigResponseDTO(adminGigResponseDTO(jobApplication.getGig()))
                .build();
    }

    private AdminReportResponseDTO reportResponseDTO(Report report) {
        return AdminReportResponseDTO.builder()
                .reportStatus(report.getReportStatus().name())
                .description(report.getDescription())
                .reason(report.getReportReason().name())
                .actionInitiatedBy(report.getActionInitiatedBy())
                .createdAt(report.getCreatedAt())
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
    private AdminTechnicalSupportResponseDTO technicalSupportResponse(TechnicalSupport technicalSupport){
        AdminUserAndClientResponseDTO adminUserAndClientResponseDTO = userAndClientResponseDTO(technicalSupport.getReporter());
        return AdminTechnicalSupportResponseDTO.builder()
                .id(technicalSupport.getId())
                .userResponseDTO(adminUserAndClientResponseDTO)
                .status(technicalSupport.getStatus())
                .subject(technicalSupport.getSubject())
                .description(technicalSupport.getDescription())
                .createdAt(technicalSupport.getCreatedAt())
                .build();
    }

    private void validateTechnicalSupportStatus(TechnicalSupportStatus newStatus ,TechnicalSupportStatus currentStatus){
        if(currentStatus.equals(TechnicalSupportStatus.PENDING) && !newStatus.equals(TechnicalSupportStatus.OPEN))
            throw new BadRequestException("Pending issue can only be changed to OPEN");

        if(currentStatus.equals(TechnicalSupportStatus.OPEN) && !newStatus.equals(TechnicalSupportStatus.IN_PROGRESS))
            throw new BadRequestException("Open issue can only be changed to IN_PROGRESS");

        if(currentStatus.equals(TechnicalSupportStatus.IN_PROGRESS) && !newStatus.equals(TechnicalSupportStatus.RESOLVED))
            throw new BadRequestException("In-progress issue can only be changed to RESOLVED");

        if (currentStatus == TechnicalSupportStatus.CLOSED)
            throw new BadRequestException("Closed issue cannot be changed");
    }

    private AdminResponseDTO adminResponseDTO(Admin admin){
        return AdminResponseDTO.builder()
                .id(admin.getId())
                .accessStatus(admin.getAdminAccessStatus())
                .contactNumber(admin.getContactNo())
                .fullName(admin.getFullName())
                .email(admin.getEmail())
                .status(admin.getAdminStatus())
                .createdAt(admin.getCreatedAt())
                .build();
    }
}
