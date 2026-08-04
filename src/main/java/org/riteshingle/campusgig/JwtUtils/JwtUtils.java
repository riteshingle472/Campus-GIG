package org.riteshingle.campusgig.JwtUtils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtUtils {
    private String secreteKey = "jnddsksbvsbvvsshgjhcmvkjsfkjjss nslkshgffsndlkffsvjbjsbnsj";

    public SecretKey getKey(){
        return Keys.hmacShaKeyFor(secreteKey.getBytes());
    }

    public String generateToken(String email, Date expiry){
        return Jwts.builder()
                .subject(email)
                .signWith(getKey())
                .issuedAt(new Date())
                .expiration(expiry)
                .compact();
    }

    public Claims extractAllClaims(String token){
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractEmail(String token){
        return extractAllClaims(token).getSubject();
    }

    public Boolean isExpire(String token){
        return extractAllClaims(token).getExpiration().before(new Date());
    }

    public Boolean validateToken(String token, UserDetails userDetails){
        String email = extractEmail(token);
        return (email.equals(userDetails.getUsername()) && ! isExpire(token));
    }

}
