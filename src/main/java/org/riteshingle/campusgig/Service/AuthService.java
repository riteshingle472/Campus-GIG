package org.riteshingle.campusgig.Service;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.riteshingle.campusgig.Enum.AvailabilityStatus;
import org.riteshingle.campusgig.Enum.Roles;
import org.riteshingle.campusgig.JwtUtils.JwtUtils;
import org.riteshingle.campusgig.Model.RefreshToken;
import org.riteshingle.campusgig.Model.Skills;
import org.riteshingle.campusgig.Model.UserEntity;
import org.riteshingle.campusgig.Model.UserSkills;
import org.riteshingle.campusgig.Repository.SkillsRepository;
import org.riteshingle.campusgig.Repository.UserSkillsRepository;
import org.riteshingle.campusgig.RequestDTO.LoginRequestDTO;
import org.riteshingle.campusgig.Repository.RefreshTokenRepository;
import org.riteshingle.campusgig.Repository.UserEntityRepository;
import org.riteshingle.campusgig.RequestDTO.CompleteProfileRequestDTO;
import org.riteshingle.campusgig.RequestDTO.EditProfileRequestDTO;
import org.riteshingle.campusgig.RequestDTO.RegisterUserRequestDTO;
import org.riteshingle.campusgig.ResponseDTO.EditResponseDTO;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthService {
    private final UserEntityRepository userEntityRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtils jwtUtils;
    private final PasswordEncoder passwordEncoder;
    private final SkillsRepository skillsRepository;
    private final UserSkillsRepository userSkillsRepository;

    private final SecureRandom random = new SecureRandom();

//    Register User
    public String registerUser(RegisterUserRequestDTO dto) {
//        Check user is already exists or not ?
        Optional<UserEntity> byEmail = userEntityRepository.findByEmail(dto.getEmail());
        if (byEmail.isPresent()) throw new RuntimeException("User already Exists with : " + dto.getEmail());

//        Create User Entity and Save in DB
        UserEntity user = UserEntity.builder()
                .email(dto.getEmail())
                .password(passwordEncoder.encode(dto.getPassword()))
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .roles(Roles.USER)
                .build();

        userEntityRepository.save(user);
        return "User saved";
    }

//    Login
    public Map<String, String> login(LoginRequestDTO dto, HttpServletResponse response) {
//        Token Expiry
        Date ACCESS_TOKEN_EXPIRY = new Date(System.currentTimeMillis() + (21 * 24 * 60 * 60 * 1000));
        Date REFRESH_TOKEN_EXPIRY = new Date(System.currentTimeMillis() + (21 * 24 * 60 * 60 * 1000));

//        Get a user by Email
        UserEntity user = userEntityRepository.findByEmail(dto.getEmail()).orElseThrow(() -> new RuntimeException("User not found"));
//        Get RefreshToken By user
        Optional<RefreshToken> byUser = refreshTokenRepository.findByUser(user);
        RefreshToken refreshToken;
        String refresh;

//        If user is present
        if (byUser.isPresent()) {
            refreshToken = byUser.get();
            boolean tokenExpired = false;

//            Check is token expire or not
            try {
                tokenExpired = jwtUtils.isExpire(refreshToken.getRefreshToken());
            } catch (Exception e) {
                tokenExpired = true;
            }

//            If is expired then generate new token and save in DB
            if (tokenExpired) {
                refresh = jwtUtils.generateToken(dto.getEmail(), REFRESH_TOKEN_EXPIRY);
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
            refresh = jwtUtils.generateToken(dto.getEmail(), REFRESH_TOKEN_EXPIRY);
            refreshToken = RefreshToken.builder().refreshToken(refresh).user(user).build();
            refreshTokenRepository.save(refreshToken);
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
            String accessToken = jwtUtils.generateToken(dto.getEmail(), ACCESS_TOKEN_EXPIRY);
            return Map.of("Access Token", accessToken);
        } catch (Exception e) {
            throw new RuntimeException("Invalid Credential");
        }
    }

//    Get current logged-in profile
    public UserEntity getCurrentProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return userEntityRepository.findByEmail(authentication.getName()).orElseThrow(() -> new RuntimeException("User not found"));
    }

//    Get public profile
    public UserEntity getPublicProfile(String email) {
        UserEntity user;
        if (email == null) user = getCurrentProfile();
        else user = userEntityRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        return user;
    }

//    Email verification OTP
    public String verifyEmailOTP(){
        return this.generateSixDigitOTP();
    }

//    Email verification
    public String verifyEmail(String otp){
        UserEntity currentProfile = this.getCurrentProfile();

        if(otp.equals("1234")) {
            currentProfile.setIsVerified(true);
            userEntityRepository.save(currentProfile) ;
            return "Email verified";
        }else {
            return "In valid OTP";
        }
    }

//    Forget Password OTP
    public String forgotPasswordOTP(){
        return this.generateSixDigitOTP();
    }

//    Forget Password
    public String forgotPassword(String otp, String password){
//        Get current logged-in user
        UserEntity currentProfile = this.getCurrentProfile();

//        Verify user OTP and System generated Redis OTP
        if(otp.equals("1234")){
//            Set new Password in Encryption
            currentProfile.setPassword(passwordEncoder.encode(password));
            userEntityRepository.save(currentProfile);
            return "OTP verified , Password Change Successfully";
        }else {
            return "Invalid OTP";
        }
    }

//    Complete Profile
    public String completeProfile(CompleteProfileRequestDTO dto){
        AvailabilityStatus availabilityStatus;

//        Check Availability Status is existed or not
        try{
            availabilityStatus = AvailabilityStatus.valueOf(dto.getAvailableStatus().trim().toUpperCase());
        }catch (Exception e){
            throw new RuntimeException("Select Correct Status");
        }

//        Set details in user profile
        UserEntity currentProfile = getCurrentProfile();

        currentProfile.setCollege(dto.getCollege());
        currentProfile.setProfileImage(dto.getProfileImage());
        currentProfile.setDepartment(dto.getDepartment());
        currentProfile.setSemester(dto.getSemester());
        currentProfile.setAvailabilityStatus(availabilityStatus);
        currentProfile.setPhoneNumber(dto.getPhoneNumber());
        currentProfile.setShortBio(dto.getShortBio());
        currentProfile.setDob(dto.getDob());

//        save profile in DB
        userEntityRepository.save(currentProfile);

        return "Profile Completed !";
    }

//    Refresh Token
    public Map<String, Object> refreshToken(String refreshToken,HttpServletResponse response){
//        Token Expiry
        Date ACCESS_TOKEN_EXPIRY = new Date(System.currentTimeMillis() + (21 * 24 * 60 * 60 * 1000));
        Date REFRESH_TOKEN_EXPIRY = new Date(System.currentTimeMillis() + (21 * 24 * 60 * 60 * 1000));

//        Get Current Logged-in profile
        UserEntity currentProfile = getCurrentProfile();
//        Get Refresh Token Entity by user
        RefreshToken refresh = refreshTokenRepository.findByUser(currentProfile).orElseThrow(() -> new RuntimeException("Refresh Token not found with : " + currentProfile.getId() + "..."));

//        Check token is expired or not
        if(jwtUtils.isExpire(refreshToken)){
            throw new RuntimeException("Token Expired");
        }

//        Generate New Access Token
        String accessToken = jwtUtils.generateToken(currentProfile.getEmail(), ACCESS_TOKEN_EXPIRY);
//        refreshToken = jwtUtils.generateToken(currentProfile.getEmail(), ACCESS_TOKEN_EXPIRY);
//
//        ResponseCookie cookie = ResponseCookie.from("RefreshToken",refreshToken)
//                .maxAge(Duration.ofDays(7))
//                .secure(false)
//                .httpOnly(true)
//                .sameSite("Lax")
//                .path("/auth/refresh-token")
//                .build();
//        response.addHeader(HttpHeaders.SET_COOKIE,cookie.toString());
//
//        refresh.setRefreshToken(refreshToken);
//        refreshTokenRepository.save(refresh);

//        Return Response
        return Map.of("Access Token",accessToken);

    }

//    Edit Profile
    public EditResponseDTO editProfile(EditProfileRequestDTO dto) {
        UserEntity user = getCurrentProfile();

        if (dto.getFirstName() != null && !dto.getFirstName().isBlank()) {
            user.setFirstName(dto.getFirstName());
        }

        if (dto.getLastName() != null && !dto.getLastName().isBlank()) {
            user.setLastName(dto.getLastName());
        }

        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            user.setEmail(dto.getEmail());
        }

        if (dto.getPhoneNumber() != null && !dto.getPhoneNumber().isBlank()) {
            user.setPhoneNumber(dto.getPhoneNumber());
        }

        if (dto.getCollege() != null && !dto.getCollege().isBlank()) {
            user.setCollege(dto.getCollege());
        }

        if (dto.getDepartment() != null && !dto.getDepartment().isBlank()) {
            user.setDepartment(dto.getDepartment());
        }

        if (dto.getSemester() != null) {
            user.setSemester(dto.getSemester());
        }

        if (dto.getProfileImage() != null && !dto.getProfileImage().isBlank()) {
            user.setProfileImage(dto.getProfileImage());
        }

        if (dto.getShortBio() != null && !dto.getShortBio().isBlank()) {
            user.setShortBio(dto.getShortBio());
        }

        if (dto.getAvailabilityStatus() != null && !dto.getAvailabilityStatus().isBlank()) {
            AvailabilityStatus availabilityStatus;

            try {
                availabilityStatus = AvailabilityStatus.valueOf(dto.getAvailabilityStatus().trim().toUpperCase());
                user.setAvailabilityStatus(availabilityStatus);
            }catch (Exception e){
                throw new RuntimeException("Status not found");
            }
        }

        userEntityRepository.save(user);

        return editResponseDTO(user);
    }

//    Add User Skills
    @Transactional
    public String addSkills(List<Long> skillIds){
//        Get current profile
        UserEntity currentProfile = this.getCurrentProfile();
//        Get User existing skills
        List<Long> userExistingSkills = userSkillsRepository.findSkillIdsByUserId(currentProfile.getId());
//        Filter skills from existing skills
        List<Long> newSkills = skillIds.stream().distinct().filter(id -> !userExistingSkills.contains(id)).toList();
//        Find Skills by ID in List
        List<Skills> skills = skillsRepository.findAllById(newSkills);

//        If List is empty then do nothing
        if(newSkills.isEmpty()) return "Changes Saved!";

//        Skill list size and Distinct skill list size if both are different then throw Exception Invalid skill selection
        if (skills.size() != newSkills.size()) {
            throw new RuntimeException("Invalid skill selected");
        }

//        Setting skills in current logged-in user profile
        List<UserSkills> userSkills = skills.stream().map(skill -> new UserSkills(currentProfile, skill)).toList();
        currentProfile.getUserSkills().addAll(userSkills);
//        save user in DB
        userEntityRepository.save(currentProfile);

        return "Changes Saved !";
    }

//    Helper methods

//    6 Digit OTP
    private String generateSixDigitOTP(){
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }

//    Edit Response DTO
    public EditResponseDTO editResponseDTO(UserEntity user){
        return EditResponseDTO.builder()
                .lastName(user.getLastName())
                .firstName(user.getFirstName())
                .college(user.getCollege())
                .department(user.getDepartment())
                .profileImage(user.getProfileImage())
                .semester(user.getSemester())
                .shortBio(user.getShortBio())
                .phoneNumber(user.getPhoneNumber())
                .AvailabilityStatus(user.getAvailabilityStatus().name())
                .dob(user.getDob().toString())
                .build();
    }

}
