package org.riteshingle.campusgig.Service;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.aspectj.weaver.ast.Test;
import org.riteshingle.campusgig.Enum.Roles;
import org.riteshingle.campusgig.Exception.*;
import org.riteshingle.campusgig.JwtUtils.JwtUtils;
import org.riteshingle.campusgig.Model.*;
import org.riteshingle.campusgig.RequestDTO.*;
import org.riteshingle.campusgig.Repository.RefreshTokenRepository;
import org.riteshingle.campusgig.Repository.UserEntityRepository;
import org.riteshingle.campusgig.ResponseDTO.EditResponseDTO;
import org.riteshingle.campusgig.ResponseDTO.UserProfileResponseDTO;
import org.springframework.beans.factory.annotation.Qualifier;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@RequestMapping("/auth")
@Transactional
public class AuthService {
    private final UserEntityRepository userEntityRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtils jwtUtils;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final RedisTemplate<String,Object> redisTemplate;
    private final AuthenticationManager authenticationManager;

    private final SecureRandom random = new SecureRandom();

//    Register User
    public void registerUser(RegisterUserRequestDTO dto) {
//        Check User is already exist or not by email
        Optional<UserEntity> byEmail = userEntityRepository.findByEmail(dto.getEmail());
//        Throw Conflict Error if User is present
        if (byEmail.isPresent()) throw new ConflictException("User already Exists with : " + dto.getEmail());

//        Set Role Client
        Set<Roles> roles = Set.of(Roles.CLIENT);

//        Create UserEntity and save in DB
        UserEntity user = UserEntity.builder()
                .email(dto.getEmail())
                .password(passwordEncoder.encode(dto.getPassword()))
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .roles(roles)
                .phoneNumber(dto.getPhoneNumber())
                .dob(dto.getDob())
                .build();

        String subject = "Welcome to Campus GIG – Let’s Get Started! \uD83C\uDF93";
        String body = "Subject: Welcome to Campus GIG – Let’s Get Started! \uD83C\uDF93\n" +
                "\n" +
                "Hello "+user.getFirstName()+" "+user.getLastName()+",\n" +
                "\n" +
                "Welcome to **Campus GIG!** \uD83C\uDF89\n" +
                "\n" +
                "We’re excited to have you join our growing community.\n" +
                "\n" +
                "**Campus GIG** is a platform that connects **students, clients, and opportunities** in one place. Whether you’re here to showcase your skills, find talented students, or discover new opportunities, you’re now part of the Campus GIG community.\n" +
                "\n" +
                "### \uD83D\uDE80 What you can do on Campus GIG\n" +
                "\n" +
                "* \uD83C\uDFAF Discover and apply for exciting GIG opportunities\n" +
                "* \uD83D\uDCBC Showcase your skills and build your profile\n" +
                "* \uD83E\uDD1D Connect with Clients and Gigs\n" +
                "* \uD83D\uDCAC Communicate through our platform\n" +
                "* \uD83D\uDCC8 Grow your experience and opportunities\n" +
                "\n" +
                "Your journey starts here. Complete your profile, explore available opportunities, and make the most of Campus GIG.\n" +
                "\n" +
                "**Welcome aboard! We’re glad to have you with us.**\n" +
                "\n" +
                "Best Regards,\n" +
                "**Team Campus GIG**\n" +
                "\n" +
                "━━━━━━━━━━━━━━━━━━━━\n" +
                "**CAMPUS GIG**\n" +
                "*Empowering Students. Connecting Opportunities.*\n" +
                "━━━━━━━━━━━━━━━━━━━━\n" +
                "\n" +
                "This is an automated email. Please do not reply directly to this email.\n";

        try {
            emailService.sendMail(user.getEmail(),subject,body);
        }catch (Exception e){
            throw  new EmailSendingException("Failed to send User Welcome email"+ e);
        }
        userEntityRepository.save(user);
    }

//    Login
    public Map<String, String> login(LoginRequestDTO dto, HttpServletResponse response) {
//        Token Expiry
        Date ACCESS_TOKEN_EXPIRY = new Date(System.currentTimeMillis() + (21 * 24 * 60 * 60 * 1000));
        Date REFRESH_TOKEN_EXPIRY = new Date(System.currentTimeMillis() + (21 * 24 * 60 * 60 * 1000));

//        Authentication Manager for Matching password
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(dto.getEmail(), dto.getPassword()));

//        Get User by Email
        UserEntity user = userEntityRepository.findByEmailWithRoles(dto.getEmail())
                .orElseThrow(() -> new UnauthorizedException("No account found with this email. Please sign up."));

//        Get Refresh Token
        Optional<RefreshToken> byUser = refreshTokenRepository.findByUser(user);
        RefreshToken refreshToken;
        String refresh;

        if (byUser.isPresent()) {
//            Get Refresh Token if exists
            refreshToken = byUser.get();
            boolean tokenExpired;

//            Check token is expired or not
            try {
                tokenExpired = jwtUtils.isExpire(refreshToken.getRefreshToken());
            } catch (Exception e) {
                tokenExpired = true;
            }

//            Generate new token and save it in DB if token is expired
            if (tokenExpired) {
                refresh = jwtUtils.generateToken(dto.getEmail(), REFRESH_TOKEN_EXPIRY, user.getRoles());
                refreshToken.setRefreshToken(refresh);
                refreshTokenRepository.save(refreshToken);
            }
//            Get existing refresh token if it didn't expire
            else {
                refresh = refreshToken.getRefreshToken();
            }
        }
//        Create new RefreshToken entity and save in DB
        else {
            refresh = jwtUtils.generateToken(dto.getEmail(), REFRESH_TOKEN_EXPIRY, user.getRoles());
            refreshToken = RefreshToken.builder().refreshToken(refresh).user(user).build();
            refreshTokenRepository.save(refreshToken);
        }

//        Create and Set refresh token in cookies for 7 days
        ResponseCookie cookie = ResponseCookie.from("RefreshToken", refresh)
                .httpOnly(true)
                .secure(false)
                .path("/api/auth/refresh-token")
                .maxAge(Duration.ofDays(7))
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

//        Generate Access token
        String accessToken = jwtUtils.generateToken(dto.getEmail(), ACCESS_TOKEN_EXPIRY, user.getRoles());
        return Map.of("Access Token", accessToken);
    }

//    Get current Logged-in Profile
    public UserEntity getCurrentProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return userEntityRepository.findByEmailWithRoles(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

//    Profile
    public UserProfileResponseDTO viewProfile(){
//        Get Current Logged-in Profile
        UserEntity currentProfile = getCurrentProfile();
//        Convert current profile into DTO
        return toResponse(currentProfile);
    }

//    Reset Password
    public void resetPassword(ResetPasswordRequestDTO dto){
//        Get Current Logged-in Profile
        UserEntity currentProfile = this.getCurrentProfile();
//        Match Old password
        if(!passwordEncoder.matches(dto.getOldPassword(),currentProfile.getPassword()))
            throw new BadRequestException("Old password is incorrect");

//        Match new and Confirm password
        if(dto.getNewPassword().equals(dto.getConfirmPassword())){
            currentProfile.setPassword(passwordEncoder.encode(dto.getNewPassword()));
            userEntityRepository.save(currentProfile);
        }
        else throw new BadRequestException("Incorrect password..");
    }

//    Email verification OTP — unchanged (relies on being logged in — confirmed intentional flow)
    public void verifyEmailOTP(){
        UserEntity currentProfile = getCurrentProfile();
        String key = "Verification:"+currentProfile.getId()+":OTP:";
        String otp = this.generateSixDigitOTP();

        redisTemplate.opsForValue().set(key,otp,5,TimeUnit.MINUTES);

        String subject = "Verify Your Email – Campus GIG";
        String body = "Hi "+currentProfile.getFirstName()+",\n" +
                "\n" +
                "Welcome to Campus GIG!\n" +
                "\n" +
                "To verify your email address and complete your registration, please use the OTP given below:\n" +
                "\n" +
                "**Your Email Verification OTP: "+otp+"**\n" +
                "\n" +
                "This OTP is valid for 5 minutes**. Please do not share this OTP with anyone.\n" +
                "\n" +
                "If you did not create a Campus GIG account, you can safely ignore this email.\n" +
                "\n" +
                "Best Regards,\n" +
                "**Team Campus GIG**\n" +
                "\n" +
                "━━━━━━━━━━━━━━━━━━━━\n" +
                "**CAMPUS GIG**\n" +
                "*Empowering Students. Connecting Opportunities.*\n" +
                "━━━━━━━━━━━━━━━━━━━━\n" +
                "\n" +
                "This is an automated email. Please do not reply directly to this email.\n";

        try {
            emailService.sendMail(currentProfile.getEmail(),subject,body);
        }catch (Exception e){
            throw  new EmailSendingException("Failed to send Verification OPT email"+ e);
        }
    }

//    Verify Email
    public String verifyEmail(String otp){
//        Get Current Logged-in Profile
        UserEntity currentProfile = this.getCurrentProfile();

        String key = "Verification:"+currentProfile.getId()+":OTP:";
        Object redisOTP = redisTemplate.opsForValue().get(key);

//        Check OTP gets expire ?
        if(redisOTP == null) throw new BadRequestException("OTP has expire , Request new OTP..");

//        convert OTP into String to verify it
        String storedOTP = redisOTP.toString();

//        Verify OTP
        if(otp.equals(storedOTP)) {
//            Set Profile isVerified True
            currentProfile.setIsVerified(true);
//            save in DB
            userEntityRepository.save(currentProfile);
            redisTemplate.delete(key);
            return "Email verified";
        } else {
            return "In valid OTP";
        }
    }

//    Forget Password OTP
    public void forgotPasswordOTP(String email){
//        Find User by Email
        UserEntity user = userEntityRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

//        Create Redis key for storing OTP in redis
        String key = "Forget_Password:"+user.getId()+":OTP:";
        String otp = this.generateSixDigitOTP();
//        After 2 minutes OTP must get expire
        redisTemplate.opsForValue().set(key, otp, 5, TimeUnit.MINUTES);

        String subject = "Password Reset OTP";
        String body = "Hi "+user.getFirstName()+",\n" +
                "\n" +
                "We received a request to reset your password for your Campus GIG account.\n" +
                "\n" +
                "Your password reset OTP is:\n" +
                "\n" +
                "** "+otp+" **\n" +
                "\n" +
                "This OTP is valid for 5 minutes**. Please do not share this OTP with anyone.\n" +
                "\n" +
                "If you did not request a password reset, you can safely ignore this email.\n" +
                "\n" +
                "Best Regards,\n" +
                "**Team Campus GIG**\n" +
                "\n" +
                "━━━━━━━━━━━━━━━━━━━━\n" +
                "**CAMPUS GIG**\n" +
                "*Empowering Students. Connecting Opportunities.*\n" +
                "━━━━━━━━━━━━━━━━━━━━\n" +
                "\n" +
                "This is an automated email. Please do not reply directly to this email.\n";

        try {
            emailService.sendMail(user.getEmail(),subject,body);
        }catch (Exception e){
            throw  new EmailSendingException("Failed to send Forget Password OTP email"+ e);
        }

    }

//    Forget Password
    public String forgotPassword(String email, String otp, String password){
//        Get User by email
        UserEntity user = userEntityRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        String key = "Forget_Password:"+user.getId()+":OTP:";
        Object redisOTP = redisTemplate.opsForValue().get(key);

        if (redisOTP == null)
            throw new BadRequestException("OTP has expired. Please request a new OTP.");

        String storedOtp = redisOTP.toString();

//        verify OTP
        if(otp.equals(storedOtp)){
//            Change Password and save it in DB
            user.setPassword(passwordEncoder.encode(password));
            userEntityRepository.save(user);
            redisTemplate.delete(key);
            return "OTP verified , Password Change Successfully";
        } else {
            throw new BadRequestException("Invalid OTP.");
        }
    }

//    Refresh Token — unchanged
    public Map<String, Object> refreshToken(String refreshToken, HttpServletResponse response){
//        Tokens Expiry
        Date ACCESS_TOKEN_EXPIRY = new Date(System.currentTimeMillis() + (21 * 24 * 60 * 60 * 1000));
        Date REFRESH_TOKEN_EXPIRY = new Date(System.currentTimeMillis() + (21 * 24 * 60 * 60 * 1000));

//        Get Current Logged-in Profile
        UserEntity currentProfile = getCurrentProfile();
//        Get Refresh Token By user
        RefreshToken refresh = refreshTokenRepository.findByUser(currentProfile)
                .orElseThrow(() -> new ResourceNotFoundException("Refresh Token not found with : " + currentProfile.getId() + "..."));

//        Throw Exception if Token is Expired
        if(jwtUtils.isExpire(refreshToken)){
            throw new RuntimeException("Token Expired");
        }

//        Generate Both Token
        String accessToken = jwtUtils.generateToken(currentProfile.getEmail(), ACCESS_TOKEN_EXPIRY, currentProfile.getRoles());
        refreshToken = jwtUtils.generateToken(currentProfile.getEmail(), REFRESH_TOKEN_EXPIRY, currentProfile.getRoles());

//        Create Refresh Token Cookie
        ResponseCookie cookie = ResponseCookie.from("RefreshToken", refreshToken)
                .maxAge(Duration.ofDays(7))
                .secure(false)
                .httpOnly(true)
                .sameSite("Lax")
                .path("/auth/refresh-token")
                .build();
//        Set Cookie in headers
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

//        Save Refresh Token
        refresh.setRefreshToken(refreshToken);
        refreshTokenRepository.save(refresh);

        return Map.of("Access Token", accessToken);
    }

//    Edit Profile
    public EditResponseDTO editProfile(EditProfileRequestDTO dto) {
//        Update User
        UserEntity user = getUpdatedUser(dto);
//        Save in DB
        userEntityRepository.save(user);
        return editResponseDTO(user);
    }

    private UserEntity getUpdatedUser(EditProfileRequestDTO dto) {
        UserEntity user = getCurrentProfile();

        if (dto.getFirstName() != null && !dto.getFirstName().isBlank())
            user.setFirstName(dto.getFirstName());

        if (dto.getLastName() != null && !dto.getLastName().isBlank())
            user.setLastName(dto.getLastName());

        if (dto.getEmail() != null && !dto.getEmail().isBlank()) user.setEmail(dto.getEmail());

        if (dto.getPhoneNumber() != null && !dto.getPhoneNumber().isBlank())
            user.setPhoneNumber(dto.getPhoneNumber());

        if(dto.getDob() != null) user.setDob(dto.getDob());

        return user;
    }

//    Upload Profile Image
    public String uploadProfileImage(MultipartFile file) throws IOException {
        UserEntity user = getCurrentProfile();

        String uploadDir = "uploads/profile-images/";
        Files.createDirectories(Paths.get(uploadDir));

        String originalName = file.getOriginalFilename();
        String extension = (originalName != null && originalName.contains("."))
                ? originalName.substring(originalName.lastIndexOf('.'))
                : "";
        String filename = "user-" + user.getId() + "-" + System.currentTimeMillis() + extension;

        Path filePath = Paths.get(uploadDir + filename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        String publicUrl = "/uploads/profile-images/" + filename;
        user.setProfileImage(publicUrl);
        userEntityRepository.save(user);

        return publicUrl;
    }

    //    Generate 6 Digit Random Number
    public String generateSixDigitOTP(){
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }

    public EditResponseDTO editResponseDTO(UserEntity user){
        return EditResponseDTO.builder()
                .lastName(user.getLastName())
                .firstName(user.getFirstName())
                .email(user.getEmail())
                .dob(user.getDob())
                .phoneNumber(user.getPhoneNumber())
                .build();
    }

    private UserProfileResponseDTO toResponse(UserEntity currentProfile) {
        return UserProfileResponseDTO.builder()
                .averageRating(currentProfile.getAverageRating())
                .createdAt(currentProfile.getCreatedAt())
                .email(currentProfile.getEmail())
                .profileImage(currentProfile.getProfileImage())
                .dob(currentProfile.getDob())
                .totalRatings(currentProfile.getTotalRatings())
                .firstName(currentProfile.getFirstName())
                .phoneNumber(currentProfile.getPhoneNumber())
                .lastName(currentProfile.getLastName())
                .id(currentProfile.getId())
                .roles(currentProfile.getRoles())
                .isVerified(currentProfile.getIsVerified())
                .build();
    }
}