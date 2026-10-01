package org.riteshingle.campusgig.Controller;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.riteshingle.campusgig.RequestDTO.*;
import org.riteshingle.campusgig.ResponseDTO.EditResponseDTO;
import org.riteshingle.campusgig.ResponseDTO.UserProfileResponseDTO;
import org.riteshingle.campusgig.Service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

import java.util.Map;

@RequestMapping("/api/auth")
@RequiredArgsConstructor
@RestController
public class AuthController {
    private final AuthService authService;

//    User register
    @PostMapping("/sign-up")
    public ResponseEntity<String> registerUser(@RequestBody RegisterUserRequestDTO dto) {
        authService.registerUser(dto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

//    Login - POST
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody LoginRequestDTO dto, HttpServletResponse response) {
        return ResponseEntity.ok(authService.login(dto, response));
    }

//    Email verification OTP API
    @GetMapping("/verification")
    public ResponseEntity<String> verifyEmailOTP() {
        return ResponseEntity.ok(authService.verifyEmailOTP());
    }

//    Email Verification API
    @PatchMapping("/verification")
    public ResponseEntity<String> verifyEmail(@RequestParam String otp) {
        return ResponseEntity.ok(authService.verifyEmail(otp));
    }

    @PatchMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequestDTO dto) {
        authService.resetPassword(dto);
        return ResponseEntity.noContent().build();
    }

//    Forgot Password OTP API
    @GetMapping("/forgot-password")
    public ResponseEntity<String> forgotPasswordOTP(@RequestParam String email) {
        return ResponseEntity.ok(authService.forgotPasswordOTP(email));
    }

//    Forgot/Change Password API
    @PatchMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestParam String email, @RequestParam String otp, @RequestParam String newPassword) {
        return ResponseEntity.ok(authService.forgotPassword(email, otp, newPassword));
    }

//    Refresh Token
    @GetMapping("/refresh-token")
    public ResponseEntity<Map<String, Object>> refreshToken(@CookieValue(name = "RefreshToken") String refreshToken, HttpServletResponse response) {
        return ResponseEntity.ok(authService.refreshToken(refreshToken, response));
    }

//    User Profile
    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponseDTO> userProfile() {
        return ResponseEntity.ok(authService.viewProfile());
    }

//    Edit Profile
    @PreAuthorize("hasRole('CLIENT') or hasRole('GIG')")
    @PatchMapping("/edit-profile")
    public ResponseEntity<EditResponseDTO> editProfile(@RequestBody EditProfileRequestDTO dto) {
        return ResponseEntity.ok(authService.editProfile(dto));
    }

    @PreAuthorize("hasRole('CLIENT') or hasRole('GIG')")
    @PostMapping("/profile/image")
    public ResponseEntity<Map<String, String>> uploadProfileImage(@RequestParam("file") MultipartFile file) throws IOException {
        String url = authService.uploadProfileImage(file);
        return ResponseEntity.ok(Map.of("profileImage", url));
    }

    @GetMapping("/test")
    public String test() {
        return "Test case";
    }
}