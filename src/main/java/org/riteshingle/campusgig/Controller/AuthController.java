package org.riteshingle.campusgig.Controller;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.riteshingle.campusgig.RequestDTO.*;
import org.riteshingle.campusgig.ResponseDTO.EditResponseDTO;
import org.riteshingle.campusgig.Service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RequestMapping("/auth")
@RequiredArgsConstructor
@RestController
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register-user")
    public ResponseEntity<String> registerUser(@RequestBody RegisterUserRequestDTO dto) {
        return ResponseEntity.ok(authService.registerUser(dto));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody LoginRequestDTO dto, HttpServletResponse response) {
        return ResponseEntity.ok(authService.login(dto, response));
    }

    @GetMapping("/email-verification-otp")
    public ResponseEntity<String> verifyEmailOTP() {
        return ResponseEntity.ok(authService.verifyEmailOTP());
    }

    @PostMapping("/verify-email")
    public ResponseEntity<String> verifyEmail(@RequestParam String otp) {
        return ResponseEntity.ok(authService.verifyEmail(otp));
    }

    @GetMapping("/forgot-password-otp")
    public ResponseEntity<String> forgotPasswordOTP() {
        return ResponseEntity.ok(authService.forgotPasswordOTP());
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestParam String otp, @RequestParam String newPassword) {
        return ResponseEntity.ok(authService.forgotPassword(otp, newPassword));
    }

    @PatchMapping("/complete-profile")
    public ResponseEntity<String> completeProfile(@RequestBody CompleteProfileRequestDTO dto) {
        return ResponseEntity.ok(authService.completeProfile(dto));
    }

    @GetMapping("/refresh-token")
    public ResponseEntity<Map<String,Object>> refreshToken(@CookieValue(name = "RefreshToken") String refreshToken,HttpServletResponse response){
        return ResponseEntity.ok(authService.refreshToken(refreshToken,response));
    }

    @PatchMapping("/edit-profile")
    public ResponseEntity<EditResponseDTO> editProfile(@RequestBody EditProfileRequestDTO dto){
        return ResponseEntity.ok(authService.editProfile(dto));
    }

    @PostMapping("/add-user-skills")
    public ResponseEntity<String> addSkills(@RequestBody AddUserSkillsRequestDTO dto){
        return ResponseEntity.ok(authService.addSkills(dto.getSkillsId()));
    }

    @GetMapping("/test")
    public String test() {
        return "Test case";
    }
}
