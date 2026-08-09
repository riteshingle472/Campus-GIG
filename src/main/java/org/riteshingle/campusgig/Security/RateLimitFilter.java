package org.riteshingle.campusgig.Security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    private Bucket createBucket(){
        Bandwidth limit = Bandwidth.builder()
                .capacity(10)                     // max 10 requests
                .refillGreedy(10, Duration.ofMinutes(1)) // every minute
                .build();

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String ip = request.getRemoteAddr();

        Bucket bucket = cache.computeIfAbsent(ip, k -> createBucket());

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");

            response.getWriter().write("""
                    {
                      "status":429,
                      "message":"Too many requests. Try again later."
                    }
                    """);
        }
    }
}
