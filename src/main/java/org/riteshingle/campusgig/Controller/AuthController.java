package org.riteshingle.campusgig.Controller;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.riteshingle.campusgig.Repository.LoginRequestDTO;
import org.riteshingle.campusgig.RequestDTO.RegisterUserRequestDTO;
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
    public ResponseEntity<String> registerUser(@RequestBody RegisterUserRequestDTO dto){
        return ResponseEntity.ok(authService.registerUser(dto));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String,String>> login(@RequestBody LoginRequestDTO dto, HttpServletResponse response){
        return ResponseEntity.ok(authService.login(dto,response));
    }

    @GetMapping("/test")
    public String test(){
        return "Test case";
    }
}
