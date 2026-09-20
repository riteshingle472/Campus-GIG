package org.riteshingle.campusgig.JwtUtils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.riteshingle.campusgig.Enum.Roles;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Transactional
public class JwtUtils {
    private String secreteKey = "jnddsksbvsbvvsshgjhcmvkjsfkjjssggjfggjfjhj  fgffjfhcfgkjlkbjhjgfguyfysdvcjhsgfgsjchsvjbb nslkshgffsndlkffsvjbjsbnsj";

    public SecretKey getKey() {
        return Keys.hmacShaKeyFor(secreteKey.getBytes());
    }

//    Generate Token
    public String generateToken(String email, Date expiry, Set<Roles> roles) {
        String role = roles.iterator().next().name();
        return Jwts.builder()
                .subject(email)
                .signWith(getKey())
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(expiry)
                .compact();
    }

//    Extract All JWT Claims
    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

//    Extract Email
    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

//    Check JWT Expiry
    public Boolean isExpire(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }

//    Validate Token
    public Boolean validateToken(String token, UserDetails userDetails) {
        String email = extractEmail(token);
        return (email.equals(userDetails.getUsername()) && !isExpire(token));
    }

//    Extract Role
    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

}
