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

import java.util.Map;

@RequestMapping("/auth")
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

    //    Login
    @GetMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody LoginRequestDTO dto, HttpServletResponse response) {
        return ResponseEntity.ok(authService.login(dto, response));
    }

    //    Email varification OTP API
    @GetMapping("/verification")
    public ResponseEntity<String> verifyEmailOTP() {
        return ResponseEntity.ok(authService.verifyEmailOTP());
    }

    //    Email Varification API
    @PatchMapping("/verification")
    public ResponseEntity<String> verifyEmail(@RequestParam String otp) {
        return ResponseEntity.ok(authService.verifyEmail(otp));
    }

    //    Forgot Password OTP API
    @GetMapping("/forgot-password")
    public ResponseEntity<String> forgotPasswordOTP() {
        return ResponseEntity.ok(authService.forgotPasswordOTP());
    }

    //    Forgot/Change Password API
    @PatchMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestParam String otp, @RequestParam String newPassword) {
        return ResponseEntity.ok(authService.forgotPassword(otp, newPassword));
    }

    //    Refresh Token
    @GetMapping("/refresh-token")
    public ResponseEntity<Map<String, Object>> refreshToken(@CookieValue(name = "RefreshToken") String refreshToken, HttpServletResponse response) {
        return ResponseEntity.ok(authService.refreshToken(refreshToken, response));
    }

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

    @GetMapping("/test")
    public String test() {
        return "Test case";
    }
}
