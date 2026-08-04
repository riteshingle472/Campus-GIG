package org.riteshingle.campusgig.Service;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.riteshingle.campusgig.Enum.Roles;
import org.riteshingle.campusgig.JwtUtils.JwtUtils;
import org.riteshingle.campusgig.Model.RefreshToken;
import org.riteshingle.campusgig.Model.UserEntity;
import org.riteshingle.campusgig.Repository.LoginRequestDTO;
import org.riteshingle.campusgig.Repository.RefreshTokenRepository;
import org.riteshingle.campusgig.Repository.UserEntityRepository;
import org.riteshingle.campusgig.RequestDTO.RegisterUserRequestDTO;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.Duration;
import java.util.Date;
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

    public String registerUser(RegisterUserRequestDTO dto) {
        Optional<UserEntity> byEmail = userEntityRepository.findByEmail(dto.getEmail());
        if (byEmail.isPresent()) throw new RuntimeException("User already Exists with : " + dto.getEmail());

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

    public Map<String, String> login(LoginRequestDTO dto, HttpServletResponse response) {
        Date ACCESS_TOKEN_EXPIRY = new Date(System.currentTimeMillis() + (24 * 60 * 60 * 1000));
        Date REFRESH_TOKEN_EXPIRY = new Date(System.currentTimeMillis() + (21 * 24 * 60 * 60 * 1000));

        UserEntity user = userEntityRepository.findByEmail(dto.getEmail()).orElseThrow(() -> new RuntimeException("User not found"));
        Optional<RefreshToken> byUser = refreshTokenRepository.findByUser(user);
        RefreshToken refreshToken;
        String refresh;

        if (byUser.isPresent()) {
            refreshToken = byUser.get();
            boolean tokenExpired = false;

            try {
                tokenExpired = jwtUtils.isExpire(refreshToken.getRefreshToken());
            } catch (Exception e) {
                tokenExpired = true;
            }

            if (tokenExpired) {
                refresh = jwtUtils.generateToken(dto.getEmail(), REFRESH_TOKEN_EXPIRY);
                refreshToken.setRefreshToken(refresh);
                refreshTokenRepository.save(refreshToken);
            } else {
                refresh = refreshToken.getRefreshToken();
            }
        } else {
            refresh = jwtUtils.generateToken(dto.getEmail(), REFRESH_TOKEN_EXPIRY);
            refreshToken = RefreshToken.builder().refreshToken(refresh).user(user).build();
            refreshTokenRepository.save(refreshToken);
        }

        ResponseCookie cookie = ResponseCookie.from("RefreshToken", refresh)
                .httpOnly(true)
                .secure(false)
                .path("/auth/refresh-token")
                .maxAge(Duration.ofDays(7))
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        try {
            String accessToken = jwtUtils.generateToken(dto.getEmail(), ACCESS_TOKEN_EXPIRY);
            return Map.of("Access Token", accessToken);
        } catch (Exception e) {
            throw new RuntimeException("Invalid Credential");
        }
    }

    public UserEntity getCurrentProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return userEntityRepository.findByEmail(authentication.getName()).orElseThrow(() -> new RuntimeException("User not found"));
    }

    public UserEntity getPublicProfile(String email) {
        UserEntity user;
        if (email == null) user = getCurrentProfile();
        else user = userEntityRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        return user;
    }
}
