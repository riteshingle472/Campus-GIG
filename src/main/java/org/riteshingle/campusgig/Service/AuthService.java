package org.riteshingle.campusgig.Service;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.riteshingle.campusgig.Enum.Roles;
import org.riteshingle.campusgig.Exception.BadRequestException;
import org.riteshingle.campusgig.Exception.ConflictException;
import org.riteshingle.campusgig.Exception.EmailSendingException;
import org.riteshingle.campusgig.Exception.ResourceNotFoundException;
import org.riteshingle.campusgig.JwtUtils.JwtUtils;
import org.riteshingle.campusgig.Model.*;
import org.riteshingle.campusgig.RequestDTO.*;
import org.riteshingle.campusgig.Repository.RefreshTokenRepository;
import org.riteshingle.campusgig.Repository.UserEntityRepository;
import org.riteshingle.campusgig.ResponseDTO.EditResponseDTO;
import org.riteshingle.campusgig.ResponseDTO.UserProfileResponseDTO;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.mail.MailException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;

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
    private final RedisTemplate<String, Object> redisTemplate;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    private final SecureRandom random = new SecureRandom();

    //    Register User
    public void registerUser(RegisterUserRequestDTO dto) {
//        Check user is already exists or not ?
        Optional<UserEntity> byEmail = userEntityRepository.findByEmail(dto.getEmail());
        if (byEmail.isPresent()) throw new ConflictException("User already Exists with : " + dto.getEmail());

        Set<Roles> roles = Set.of(Roles.CLIENT);

//        Create User Entity and Save in DB
        UserEntity user = UserEntity.builder()
                .email(dto.getEmail())
                .password(passwordEncoder.encode(dto.getPassword()))
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .roles(roles)
                .phoneNumber(dto.getPhoneNumber())
                .dob(dto.getDob())
                .build();

        userEntityRepository.save(user);

        String subject = "Welcome to Campus GIG!";
        String body = """
                        Hi %s,
                
                        Welcome to Campus GIG! 🎉
                
                        We're excited to have you with us!
                
                        Campus GIG is a platform where students can discover opportunities, showcase their skills, create gigs, and connect with other students.
                
                        You can now explore gigs, find opportunities, and start building your journey with Campus GIG.
                
                        We hope you have a great experience with us!
                
                        Regards,
                        **Campus GIG Team**
                        Connecting Students. Creating Opportunities.
                
                """.formatted(dto.getFirstName() + " " + dto.getLastName());

        try {
//            Sending Welcome email
//            notificationService.sendMail(dto.getEmail(), subject, body);
        } catch (MailException e) {
            throw new EmailSendingException("Failed to send mail..");
        }
    }

//    Login
    public Map<String, String> login(LoginRequestDTO dto, HttpServletResponse response) {
//        Token Expiry
        Date ACCESS_TOKEN_EXPIRY = new Date(System.currentTimeMillis() + (24 * 24 * 60 * 60 * 1000));
        Date REFRESH_TOKEN_EXPIRY = new Date(System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000));

        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(dto.getEmail(),dto.getPassword()));

//        Get a user by Email
        UserEntity user = userEntityRepository.findByEmailWithRoles(dto.getEmail()).orElseThrow(() -> new ResourceNotFoundException("User not found"));
//        Get RefreshToken By user
        Optional<RefreshToken> byUser = refreshTokenRepository.findByUser(user);
        RefreshToken refreshToken;
        String refresh;

//        If user is present
        if (byUser.isPresent()) {
            refreshToken = byUser.get();
            boolean tokenExpired;

//            Check is token expire or not
            try {
                tokenExpired = jwtUtils.isExpire(refreshToken.getRefreshToken());
            } catch (Exception e) {
                tokenExpired = true;
            }

//            If is expired then generate new token and save in DB
            if (tokenExpired) {
                refresh = jwtUtils.generateToken(dto.getEmail(), REFRESH_TOKEN_EXPIRY, user.getRoles());
                refreshToken.setRefreshToken(refresh);
                refreshTokenRepository.save(refreshToken);
            }
//            If token is not expired then get existing one
            else {
                refresh = refreshToken.getRefreshToken();
            }
        }
//        Create a new entity and Generate token and save in DB
        else {
            refresh = jwtUtils.generateToken(dto.getEmail(), REFRESH_TOKEN_EXPIRY, user.getRoles());
            refreshToken = RefreshToken.builder().refreshToken(refresh).user(user).build();
            refreshTokenRepository.save(refreshToken);
        }

//        Set refresh token in cookie
        ResponseCookie cookie = ResponseCookie.from("RefreshToken", refresh)
                .httpOnly(true)
                .secure(false)
                .path("/api/auth/refresh-token")
                .maxAge(Duration.ofDays(7))
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

//        Generate and Return Access token
        String accessToken = jwtUtils.generateToken(dto.getEmail(), ACCESS_TOKEN_EXPIRY, user.getRoles());
        return Map.of("Access Token", accessToken);
    }

//    Get current logged-in profile
    public UserEntity getCurrentProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return userEntityRepository.findByEmail(authentication.getName()).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

//    Get public profile
    public UserEntity getPublicProfile(String email) {
        UserEntity user;
        if (email == null) user = getCurrentProfile();
        else
            user = userEntityRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return user;
    }

//    View Profile
    public UserProfileResponseDTO viewProfile() {
//        Get Current Logged-in profile
        UserEntity currentProfile = getCurrentProfile();
//        Creating Response
        return toResponse(currentProfile);
    }

    //    Email verification OTP
    public void verifyEmailOTP() {
//        Get Current Logged-in User
        UserEntity currentProfile = this.getCurrentProfile();

//        Check ( ) -> Is User is verified or not
        if (currentProfile.getIsVerified()) throw new BadRequestException("Account is already verified ..");

//        Creating Redis Key
        String key = "verification:OTP:" + currentProfile.getId();
//        OTP
        String otp = setOtpInRedis(key);

        String subject = "Verify Your Email – Campus GIG";
        String body = """
                    Hi %s,
                
                    Your Campus GIG account has been successfully registered.
                
                    To activate your account and start using Campus GIG, please verify your email address using the OTP below:
                
                    **Verification OTP: %s **
                
                    This OTP is valid for **2 minutes**. Please do not share this OTP with anyone.
                
                    If you did not create an account on Campus GIG, please ignore this email.
                
                    Regards,
                    **Campus GIG Team**
                    Connecting Students. Creating Opportunities.
                """.formatted(currentProfile.getFirstName() + " " + currentProfile.getLastName(), otp);

        try {
//            Sending OTP in Email
//            notificationService.sendMail(currentProfile.getEmail(), subject, body);
        } catch (MailException e) {
            throw new EmailSendingException("Failed to send mail..");
        }
    }

//    Email verification
    public String verifyEmail(String otp) {
//        Get Current Logged-in Profile
        UserEntity currentProfile = this.getCurrentProfile();
//        Creating Redis Key
//        String key = "verification:OTP:" + currentProfile.getId() + ":" + otp;
////        Get OTP from Redis
//        Object redisOtp = redisTemplate.opsForValue().get(key);
//
////        Check OTP is null ?
//        if (redisOtp == null) {
//            throw new ResourceNotFoundException("OTP expired or not found");
//        }
////        Verify User OTP
//        if (!redisOtp.toString().equals(otp)) {
//            throw new BadRequestException("Invalid OTP");
//        }

        if(!otp.equals("1234"))
            throw new BadRequestException("Invalid OTP");

        currentProfile.setIsVerified(true);
//        currentProfile.getRoles().clear();
//        currentProfile.getRoles().add(Roles.CLIENT);
        userEntityRepository.save(currentProfile);
//        redisTemplate.delete(key);
        return "Email verified successfully";
    }

    //    Forget Password OTP
    public void forgotPasswordOTP(String email) {
//        Fetch User by Email
        UserEntity userEntity = userEntityRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("Account not found .."));
//        Creating Key for redis
        String key = "verification:OTP:" + userEntity.getId();
//        OTP
        String otp = setOtpInRedis(key);

        String subject = "Password Reset OTP – Campus GIG";
        String body = """
                    Hi %s,
                    
                    We received a request to reset the password for your Campus GIG account.
                    
                    Your password reset OTP is:
                    
                    **%s**
                    
                    This OTP is valid for **2 minutes**. Please do not share this OTP with anyone.
                    
                    If you did not request a password reset, you can safely ignore this email. Your account remains secure.
                    
                    Regards,
                    **Campus GIG Team**
                    Connecting Students. Creating Opportunities.
                """.formatted(userEntity.getFirstName() + " " + userEntity.getLastName(), otp);

        try {
//            Sending Email
//            notificationService.sendMail(userEntity.getEmail(), subject, body);
        } catch (MailException e) {
            throw new EmailSendingException("Failed to send mail..");
        }
    }

//    Forget Password
    public String forgotPassword(String otp, String password, String email) {
//        Fetch User By Email
        UserEntity userEntity = userEntityRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("Account not found .."));
//        Creating Redis Key
//        String key = "verification:OTP:" + userEntity.getId() + ":" + otp;
//
////        Get OTP from Redis
//        Object redisOtp = redisTemplate.opsForValue().get(key);
//
////        Check OTP is not null
//        if (redisOtp == null)
//            throw new ResourceNotFoundException("OTP expired or not found");
//
////        Verify User OTP
//        if (!redisOtp.toString().equals(otp))
//            throw new BadRequestException("Invalid OTP");

        if(!otp.equals("1234"))
            throw new BadRequestException("Invalid OTP");

//        Setting new Password and in DB
        userEntity.setPassword(passwordEncoder.encode(password));
        userEntityRepository.save(userEntity);
//        redisTemplate.delete(key);
        return "Email verified successfully";
    }

//    Refresh Token
    public Map<String, Object> refreshToken(String refreshToken, HttpServletResponse response) {
//        Token Expiry
        Date ACCESS_TOKEN_EXPIRY = new Date(System.currentTimeMillis() + (15 * 60 * 1000));
        Date REFRESH_TOKEN_EXPIRY = new Date(System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000));

//        Get Current Logged-in profile
        UserEntity currentProfile = getCurrentProfile();
//        Get Refresh Token Entity by user
        RefreshToken refresh = refreshTokenRepository.findByUser(currentProfile).orElseThrow(() -> new ResourceNotFoundException("Refresh Token not found with : " + currentProfile.getId() + "..."));

//        Check token is expired or not
        if (jwtUtils.isExpire(refreshToken)) {
            throw new RuntimeException("Token Expired");
        }

//        Generate New Access & Refresh Token
        String accessToken = jwtUtils.generateToken(currentProfile.getEmail(), ACCESS_TOKEN_EXPIRY, currentProfile.getRoles());
        refreshToken = jwtUtils.generateToken(currentProfile.getEmail(), REFRESH_TOKEN_EXPIRY, currentProfile.getRoles());

//        Set Refresh token in Cookie
        ResponseCookie cookie = ResponseCookie.from("RefreshToken", refreshToken)
                .maxAge(Duration.ofDays(7))
                .secure(false)
                .httpOnly(true)
                .sameSite("Lax")
                .path("/api/auth/refresh-token")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

//        Set Refresh Token in DB
        refresh.setRefreshToken(refreshToken);
        refreshTokenRepository.save(refresh);

//        Return Response
        return Map.of("Access Token", accessToken);
    }

//    Edit Profile
    public EditResponseDTO editProfile(EditProfileRequestDTO dto) {
        UserEntity user = getUpdatedUser(dto);
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

        if (dto.getDob() != null) user.setDob(dto.getDob());

        return user;
    }

//    Helper methods

//    6 Digit OTP
    private String generateSixDigitOTP() {
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }

    //    Edit Response DTO
    public EditResponseDTO editResponseDTO(UserEntity user) {
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
                .build();
    }

    private String setOtpInRedis(String key) {
        String otp = this.generateSixDigitOTP();
        key = key + ":" + otp;
        redisTemplate.opsForValue().set(key, otp);
        redisTemplate.expire(key, Duration.of(2, TimeUnit.MINUTES.toChronoUnit()));
        return otp;
    }
}
