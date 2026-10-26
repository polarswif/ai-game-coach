package com.gamecoach.security;

import com.gamecoach.persistence.SavedMatchRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * Caps screenshot uploads per account per day (UTC), checked before the paid Claude call. Runs as an
 * interceptor on POST /api/rivals/matches and /api/overwatch/matches (see WebConfig).
 */
@Component
public class UploadQuota implements HandlerInterceptor {

    private final SavedMatchRepository matches;
    private final AuthProperties properties;

    public UploadQuota(SavedMatchRepository matches, AuthProperties properties) {
        this.matches = matches;
        this.properties = properties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!"POST".equals(request.getMethod())) {
            return true;
        }
        Optional<String> user = CurrentUser.username();
        if (user.isEmpty()) {
            return true;   // Spring Security has already turned away anyone not signed in
        }
        Instant startOfDay = Instant.now().truncatedTo(ChronoUnit.DAYS);
        long today = matches.countByOwnerAndUploadedAtAfter(user.get(), startOfDay);
        if (today < properties.uploadsPerDay()) {
            return true;
        }
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.getWriter().write("{\"status\":429,\"detail\":\"You've used today's " + properties.uploadsPerDay()
                + " uploads. The limit resets at midnight UTC.\"}");
        return false;
    }
}
