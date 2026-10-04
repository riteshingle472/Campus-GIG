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
import org.springframework.data.redis.core.RedisTemplate;
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
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminService {
    private final JwtUtils jwtUtils;
    private final AuthService authService;
    private final EmailService emailService;
    private final GigRepository gigRepository;
    private final JobRepository jobRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminRepository adminRepository;
    private final ReportRepository reportRepository;
    private final ContractRepository contractRepository;
    private final UserEntityRepository userEntityRepository;
    private final UserSkillsRepository userSkillsRepository;
    private final RedisTemplate<String,Object> redisTemplate;
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
        if (byEmail.isPresent()){
            Admin admin = byEmail.get();

//            throw new ConflictException("Admin already Exists with : " + dto.getEmail());
        }

//        Set Role Admin
        Set<Roles> roles = Set.of(Roles.ADMIN);

//        Creating Admin and save in DB
        Admin admin = Admin.builder()
                .password(passwordEncoder.encode(dto.getPassword()))
                .email(dto.getEmail())
                .fullName(dto.getFullName())
                .contactNo(dto.getContactNo())
                .roles(roles)
                .adminStatus(AdminStatus.PENDING)
                .adminAccessStatus(AdminAccessStatus.PENDING)
                .build();

        if(mainAdminEmail.equals(admin.getEmail())){
            admin.setAdminStatus(AdminStatus.ACTIVE);
            admin.setAdminAccessStatus(AdminAccessStatus.ALLOWED);
        }

        adminRepository.save(admin);

        String subject = "Welcome to Campus GIG – Admin Registration Successful";
        String body = "Hi "+admin.getFullName()+",\n" +
                "\n" +
                "Welcome to Campus GIG!\n" +
                "\n" +
                "Your Admin account has been successfully registered.\n" +
                "\n" +
                "Your account status is currently **PENDING**. Once your account is approved by the authorized Admin, you will be able to log in to the Campus GIG web application using your registered credentials.\n" +
                "\n" +
                "You will receive a confirmation email once your account has been approved.\n" +
                "\n" +
                "Thank you for joining Campus GIG.\n" +
                "\n" +
                "Best Regards,\n" +
                "**Team Campus GIG**\n" +
                "\n" +
                "━━━━━━━━━━━━━━━━━━━━\n" +
                "**CAMPUS GIG**\n" +
                "━━━━━━━━━━━━━━━━━━━━\n" +
                "\n" +
                "This is an automated email. Please do not reply directly to this email.\n";

        try {
//            emailService.sendMail(admin.getEmail(),subject,body);
        }catch (Exception e){
            throw  new EmailSendingException("Failed to send Welcome email"+ e);
        }
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

        if(!admin.getAdminStatus().equals(AdminStatus.ACTIVE))
              throw new ForbiddenException("You aren't Login to Admin Panel Your Admin Status is : "+admin.getAdminStatus());

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

//    Get Current Logged-in Admin
    public Admin getCurrentAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return adminRepository.findByEmailWithRoles(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

//    Admins
    public List<AdminResponseDTO> admins(Pageable pageable,String keyword,String status){
//        Get current logged-in Admin
        Admin currentAdmin = getCurrentAdmin();

//        Only admin can see  all admins
        if(!currentAdmin.getEmail().equals(mainAdminEmail))
            throw new ForbiddenException("You don't have permission to view Admins");

//        Validate Admin
        AdminAccessStatus adminAccessStatus = null;

        if(status != null){
            try {adminAccessStatus = AdminAccessStatus.valueOf(status.trim().toUpperCase());}
            catch(InvalidStatusException exception){ throw new InvalidStatusException("Invalid status : "+status.trim().toUpperCase());}
        }
//        Fetch all admin by keyword (ALLOWED,PENDING,DENIED)
        List<Admin> admins = adminRepository.findAdminByKeywords(adminAccessStatus, keyword, pageable);
//        convert and return Admin list into AdminResponseDTO list
        return admins.stream().map(this::adminResponseDTO).toList();
    }

//    Get Current Logged-in Admin Profile
    public AdminResponseDTO adminProfile(){
        return adminResponseDTO(this.getCurrentAdmin());
    }

//    Get Admin Profile by ID
    public AdminResponseDTO adminProfileByID(Long id){
//        Get Current Logged-in Admin
        Admin currentAdmin = this.getCurrentAdmin();

//        Check Only Main admin can see Admin's Profile
        if(!currentAdmin.getEmail().equals(mainAdminEmail))
            throw new BadRequestException("You aren't allowed to View Profile");

//        Find Admin by ID
        Admin admin = adminRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Admin not found by ID : " + id));
//        Convert Admin into DTO and return DTO
        return adminResponseDTO(admin);
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
        if(currentAdmin.getEmail().equals(admin.getEmail()))
            throw new BadRequestException("You can't perform this action on yourself");

//        Validate admin access status
        AdminAccessStatus adminAccessStatus;
        try {adminAccessStatus = AdminAccessStatus.valueOf(status.trim().toUpperCase());}
        catch (InvalidStatusException exception){throw new InvalidStatusException("Invalid Admin Status : "+status.trim().toUpperCase());}

//        Admin can't set status to pending
        if(adminAccessStatus == AdminAccessStatus.PENDING)
            throw new BadRequestException("Invalid Status Selection : "+adminAccessStatus);

//        set and save changes
        if(adminAccessStatus == AdminAccessStatus.BLOCKED){
            admin.setAdminStatus(AdminStatus.BLOCKED);
            admin.setAdminAccessStatus(adminAccessStatus);
        }else if(adminAccessStatus == AdminAccessStatus.DENIED){
            admin.setAdminStatus(AdminStatus.BLOCKED);
            admin.setAdminAccessStatus(adminAccessStatus);
        }else if(adminAccessStatus == AdminAccessStatus.ALLOWED){
            admin.setAdminStatus(AdminStatus.ACTIVE);
            admin.setAdminAccessStatus(adminAccessStatus);
        }

        adminRepository.save(admin);

        String subject;
        String body;
        if(adminAccessStatus.equals(AdminAccessStatus.ALLOWED)){
            subject = "Admin Account Approved – Campus GIG";
            body = "Hi "+admin.getFullName()+",\n" +
                    "\n" +
                    "Congratulations! \uD83C\uDF89\n" +
                    "\n" +
                    "We are pleased to inform you that your **Campus GIG Admin account has been successfully approved**.\n" +
                    "\n" +
                    "You can now log in to the **Campus GIG Admin Panel** using your registered credentials and start managing the platform.\n" +
                    "\n" +
                    "Welcome to the Campus GIG Admin Team! We look forward to having you contribute to the growth and management of Campus GIG.\n" +
                    "\n" +
                    "Best Regards,\n" +
                    "**Team Campus GIG**\n" +
                    "\n" +
                    "━━━━━━━━━━━━━━━━━━━━\n" +
                    "**CAMPUS GIG**\n" +
                    "━━━━━━━━━━━━━━━━━━━━\n" +
                    "\n" +
                    "This is an automated email. Please do not reply directly to this email.\n";
        }else if(adminAccessStatus.equals(AdminAccessStatus.DENIED)) {
            subject = "Admin Account Denied – Campus GIG";
            body =  "Hi "+admin.getFullName()+",\n" +
                    "\n" +
                    "We would like to inform you that your **Campus GIG Admin access has been denied**.\n" +
                    "\n" +
                    "You are currently **not authorized to access the Campus GIG Admin Panel** using this account.\n" +
                    "\n" +
                    "If you believe this decision was made by mistake or you require further information regarding your admin access, please contact the Campus GIG support team.\n" +
                    "\n" +
                    "Thank you for your understanding.\n" +
                    "\n" +
                    "Best Regards,\n" +
                    "**Team Campus GIG**\n" +
                    "\n" +
                    "━━━━━━━━━━━━━━━━━━━━\n" +
                    "**CAMPUS GIG**\n" +
                    "━━━━━━━━━━━━━━━━━━━━\n" +
                    "\n" +
                    "This is an automated email. Please do not reply directly to this email.\n";
        }else {
            subject = "Admin Account Blocked – Campus GIG";
            body = "Hi "+admin.getFullName()+",\n" +
                    "\n" +
                    "We would like to inform you that your **Campus GIG Admin account has been blocked**.\n" +
                    "\n" +
                    "As a result, you are currently **unable to access the Campus GIG Admin Panel** using this account.\n" +
                    "\n" +
                    "If you believe your account has been blocked by mistake or you require further information regarding this action, please contact the Campus GIG support team.\n" +
                    "\n" +
                    "Thank you for your understanding.\n" +
                    "\n" +
                    "Best Regards,\n" +
                    "**Team Campus GIG**\n" +
                    "\n" +
                    "━━━━━━━━━━━━━━━━━━━━\n" +
                    "**CAMPUS GIG**\n" +
                    "━━━━━━━━━━━━━━━━━━━━\n" +
                    "\n" +
                    "This is an automated email. Please do not reply directly to this email.\n";
        }

        try {
//            emailService.sendMail(admin.getEmail(),subject,body);
        }catch (Exception e){
            throw  new EmailSendingException("Failed to send technical support confirmation email"+ e);
        }
    }

    public void forgetPasswordOTP(String email){
//        Find Admin by Email
        Admin admin = adminRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("Admin not found ..."));
//        Generate 6 Digit OTP
        String otp = authService.generateSixDigitOTP();
//        Creating Redis Key for storing OTP in Redis
        String key = "Forgot_Password:"+admin.getId()+":OTP:";

//        Set OTP in redis
        redisTemplate.opsForValue().set(key,otp,3, TimeUnit.MINUTES);

//        Email Subject and Body to sent OTP on mail
        String subject = "Reset Password OTP";
        String body = "Hi "+admin.getFullName()+",\n" +
                "\n" +
                "We received a request to reset your password for your Campus GIG account.\n" +
                "\n" +
                "Your password reset OTP is:\n" +
                "\n" +
                "** "+otp+" **\n" +
                "\n" +
                "This OTP is valid for 5 minutes**. Please do not share this OTP with anyone.\n" +
                "\n" +
                "If you didn't request a password reset, you can safely ignore this email.\n" +
                "\n" +
                "Best Regards,\n" +
                "**Team Campus GIG**\n" +
                "\n" +
                "━━━━━━━━━━━━━━━━━━━━\n" +
                "**CAMPUS GIG**\n" +
                "━━━━━━━━━━━━━━━━━━━━\n" +
                "\n" +
                "This is an automated email. Please do not reply directly to this email.\n";

        try {
            emailService.sendMail(admin.getEmail(),subject,body);
        }catch (Exception e){
            throw  new EmailSendingException("Failed to send technical support confirmation email"+ e);
        }
    }

    public void forgotPassword(String email,String otp,String newPassword){
        Admin admin = adminRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("Admin not found ..."));
        String key = "Forgot_Password:"+admin.getId()+":OTP:";

        Object redisOTP = redisTemplate.opsForValue().get(key);

        if (redisOTP == null)
            throw new BadRequestException("OTP has expired. Please request a new OTP.");

        String storedOtp = redisOTP.toString();

//        verify OTP
        if(otp.equals(storedOtp)){
//            Change Password and save it in DB
            admin.setPassword(passwordEncoder.encode(newPassword));
            adminRepository.save(admin);
            redisTemplate.delete(key);
        } else {
            throw new BadRequestException("Invalid OTP.");
        }
    }

    public void resetPassword(String oldPassword,String newPassword,String confirmPassword) {
        Admin currentAdmin = this.getCurrentAdmin();

        if(!passwordEncoder.matches(oldPassword,currentAdmin.getPassword()))
            throw new BadRequestException("Old password is incorrect");

        if(newPassword.equals(confirmPassword)) {
            currentAdmin.setPassword(passwordEncoder.encode(newPassword));
            adminRepository.save(currentAdmin);
        }
        else throw new BadRequestException("Incorrect password..");
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
//        Get Current Logged-in Admin
        Admin currentAdmin = this.getCurrentAdmin();

//        Check Admin is Active And Allowed
        if (!AdminStatus.ACTIVE.equals(currentAdmin.getAdminStatus()) || !currentAdmin.getAdminAccessStatus().equals(AdminAccessStatus.ALLOWED))
            throw new UnauthorizedException("Your admin account is not active or approved. You can't do any changes & access the Admin Panel.");

        GIG gig = gigRepository.findById(gigId).orElseThrow(() -> new ResourceNotFoundException("GIG not found by ID : " + gigId));
        return adminGigResponseDTO(gig);
    }

//    Fetch All GIGs
    public List<AdminGigResponseDTO> gigs(Pageable pageable) {
//        Get Current Logged-in Admin
        Admin currentAdmin = this.getCurrentAdmin();

//        Check Admin is Active And Allowed
        if (!AdminStatus.ACTIVE.equals(currentAdmin.getAdminStatus()) || !currentAdmin.getAdminAccessStatus().equals(AdminAccessStatus.ALLOWED))
            throw new UnauthorizedException("Your admin account is not active or approved. You can't do any changes & access the Admin Panel.");

        return gigRepository.findAll(pageable).stream().map(this::adminGigResponseDTO).toList();
    }

//    Get Client by ID
    public AdminUserAndClientResponseDTO client(Long clientId) {
//        Get Current Logged-in Admin
        Admin currentAdmin = this.getCurrentAdmin();

//        Check Admin is Active And Allowed
        if (!AdminStatus.ACTIVE.equals(currentAdmin.getAdminStatus()) || !currentAdmin.getAdminAccessStatus().equals(AdminAccessStatus.ALLOWED))
            throw new UnauthorizedException("Your admin account is not active or approved. You can't do any changes & access the Admin Panel.");

        UserEntity userEntity = userEntityRepository.findById(clientId).orElseThrow(() -> new ResourceNotFoundException("Client not found by ID : "+clientId));
        return userAndClientResponseDTO(userEntity);
    }

//    Fetch All Clients
    public List<AdminUserAndClientResponseDTO> clients(Pageable pageable) {
        Admin currentAdmin = this.getCurrentAdmin();

        if (!AdminStatus.ACTIVE.equals(currentAdmin.getAdminStatus()) || !currentAdmin.getAdminAccessStatus().equals(AdminAccessStatus.ALLOWED))
            throw new UnauthorizedException("Your admin account is not active or approved. You can't do any changes & access the Admin Panel.");

        return userEntityRepository.findAll(pageable).stream().map(this::userAndClientResponseDTO).toList();
    }

//    Get Job by ID
    public AdminJobResponseDTO job(Long jobId) {
//        Get Current Logged-in Admin
        Admin currentAdmin = this.getCurrentAdmin();

//        Check Admin is Active And Allowed
        if (!AdminStatus.ACTIVE.equals(currentAdmin.getAdminStatus()) || !currentAdmin.getAdminAccessStatus().equals(AdminAccessStatus.ALLOWED))
            throw new UnauthorizedException("Your admin account is not active or approved. You can't do any changes & access the Admin Panel.");

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
//        Get Current Logged-in Admin
        Admin currentAdmin = this.getCurrentAdmin();

//        Check Admin is Active And Allowed
        if (!AdminStatus.ACTIVE.equals(currentAdmin.getAdminStatus()) || !currentAdmin.getAdminAccessStatus().equals(AdminAccessStatus.ALLOWED))
            throw new UnauthorizedException("Your admin account is not active or approved. You can't do any changes & access the Admin Panel.");

        List<Job> jobs = jobRepository.findAll(pageable).getContent();
//        Convert into JobResponseDTO and Return
        return jobs.stream().map(this::jobResponseDTO).toList();
    }

//    Get Job Application by ID
    public AdminJobApplicationResponseDTO jobApplication(Long jobApplicationId) {
//        Get Current Logged-in Admin
        Admin currentAdmin = this.getCurrentAdmin();

//        Check Admin is Active And Allowed
        if (!AdminStatus.ACTIVE.equals(currentAdmin.getAdminStatus()) || !currentAdmin.getAdminAccessStatus().equals(AdminAccessStatus.ALLOWED))
            throw new UnauthorizedException("Your admin account is not active or approved. You can't do any changes & access the Admin Panel.");

        JobApplication jobApplication = jobApplicationRepository.findById(jobApplicationId).orElseThrow(() -> new ResourceNotFoundException("Job Application not found by Job Application ID : "+jobApplicationId));
        return jobApplicationResponseDTO(jobApplication);
    }

//    Fetch All Job Application
    public List<AdminJobApplicationListResponseDTO> jobApplications(Pageable pageable){
//        Get Current Logged-in Admin
        Admin currentAdmin = this.getCurrentAdmin();

//        Check Admin is Active And Allowed
        if (!AdminStatus.ACTIVE.equals(currentAdmin.getAdminStatus()) || !currentAdmin.getAdminAccessStatus().equals(AdminAccessStatus.ALLOWED))
            throw new UnauthorizedException("Your admin account is not active or approved. You can't do any changes & access the Admin Panel.");

        List<JobApplication> content = jobApplicationRepository.findAll(pageable).getContent();
        return content.stream().map(this::jobApplicationListResponseDTO).toList();
    }

//    Fetch All Reports
    public List<AdminReportListResponseDTO> reports(Pageable pageable){
//        Get Current Logged-in Admin
        Admin currentAdmin = this.getCurrentAdmin();

//        Check Admin is Active And Allowed
        if (!AdminStatus.ACTIVE.equals(currentAdmin.getAdminStatus()) || !currentAdmin.getAdminAccessStatus().equals(AdminAccessStatus.ALLOWED))
            throw new UnauthorizedException("Your admin account is not active or approved. You can't do any changes & access the Admin Panel.");

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
//        Get Current Logged-in Admin
        Admin currentAdmin = this.getCurrentAdmin();

//        Check Admin is Active And Allowed
        if (!AdminStatus.ACTIVE.equals(currentAdmin.getAdminStatus()) || !currentAdmin.getAdminAccessStatus().equals(AdminAccessStatus.ALLOWED))
            throw new UnauthorizedException("Your admin account is not active or approved. You can't do any changes & access the Admin Panel.");

        Report report = reportRepository.findById(reportId).orElseThrow(() -> new ResourceNotFoundException("Report not found by ID : "+reportId));
        return reportResponseDTO(report);
    }

//    Update Report
    public void updateReport(String status,Long id){
//        Get Current Logged-in Admin
        Admin currentAdmin = this.getCurrentAdmin();

//        Check Admin is Active And Allowed
        if (!AdminStatus.ACTIVE.equals(currentAdmin.getAdminStatus()) || !currentAdmin.getAdminAccessStatus().equals(AdminAccessStatus.ALLOWED))
            throw new UnauthorizedException("Your admin account is not active or approved. You can't do any changes & access the Admin Panel.");

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

    private void validateReportStatusUpdate(ReportStatus currentStatus,ReportStatus newStatus) {
        if (currentStatus == ReportStatus.RESOLVED || currentStatus == ReportStatus.REJECTED)
            throw new BadRequestException("Report status cannot be updated once it is resolved or rejected");

        if (currentStatus == newStatus)
            throw new BadRequestException("Report is already in " + currentStatus);

        if (currentStatus == ReportStatus.PENDING && newStatus != ReportStatus.UNDER_REVIEW)
            throw new BadRequestException("Pending report must first be moved to UNDER_REVIEW");
    }

//    Get Technical Issue by ID
    public AdminTechnicalSupportResponseDTO technicalSupport(Long id){
//        Get Current Logged-in Admin
        Admin currentAdmin = this.getCurrentAdmin();

//        Check Admin is Active And Allowed
        if (!AdminStatus.ACTIVE.equals(currentAdmin.getAdminStatus()) || !currentAdmin.getAdminAccessStatus().equals(AdminAccessStatus.ALLOWED))
            throw new UnauthorizedException("Your admin account is not active or approved. You can't do any changes & access the Admin Panel.");

        TechnicalSupport technicalSupport = technicalSupportRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Technical Issue not found.."));
        return technicalSupportResponse(technicalSupport);
    }

//    Get Technical Issues
    public List<AdminTechnicalSupportResponseDTO> technicalSupports(Pageable pageable){
//        Get Current Logged-in Admin
        Admin currentAdmin = this.getCurrentAdmin();

//        Check Admin is Active And Allowed
        if (!AdminStatus.ACTIVE.equals(currentAdmin.getAdminStatus()) || !currentAdmin.getAdminAccessStatus().equals(AdminAccessStatus.ALLOWED))
            throw new UnauthorizedException("Your admin account is not active or approved. You can't do any changes & access the Admin Panel.");

        List<TechnicalSupport> content = technicalSupportRepository.findAll(pageable).getContent();
        return content.stream().map(this::technicalSupportResponse).toList();
    }

    public void updateTechnicalIssue(Long id ,String status){
//        Get Current Logged-in Admin
        Admin currentAdmin = this.getCurrentAdmin();

//        Check Admin is Active And Allowed
        if (!AdminStatus.ACTIVE.equals(currentAdmin.getAdminStatus()) || !currentAdmin.getAdminAccessStatus().equals(AdminAccessStatus.ALLOWED))
            throw new UnauthorizedException("Your admin account is not active or approved. You can't do any changes & access the Admin Panel.");

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

        UserEntity reporter = technicalSupport.getReporter();

        if(supportStatus.equals(TechnicalSupportStatus.RESOLVED)){
            String subject = "Technical Issue Resolved – Campus GIG";
            String body = "Hello, "+reporter.getFirstName()+"\n" +
                    "\n" +
                    "Your technical issue has been successfully resolved.\n" +
                    "\n" +
                    "You can now use the Campus GIG web application freely. If you face any other issue, please feel free to contact our support team.\n" +
                    "\n" +
                    "Thank you for your patience.\n" +
                    "\n" +
                    "Best Regards,\n" +
                    "Campus GIG Support Team\n" +
                    "\n" +
                    "━━━━━━━━━━━━━━━━━━━━\n" +
                    "**CAMPUS GIG**\n" +
                    "━━━━━━━━━━━━━━━━━━━━\n" +
                    "\n" +
                    "This is an automated email. Please do not reply directly to this email.\n";

            try {
//                emailService.sendMail(reporter.getEmail(),subject,body);
            }catch (Exception e){
                throw  new EmailSendingException("Failed to send technical support email"+ e);
            }
        }
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
