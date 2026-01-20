package com.yatrika.shared.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class GuestInterceptor implements HandlerInterceptor {

    private final GuestContext guestContext;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        // Check if user is authenticated via Spring Security
        boolean isAuthenticated = request.getUserPrincipal() != null;

        if (!isAuthenticated) {
            guestContext.setGuest(true);
            guestContext.setGuestId(generateGuestId(request));
            guestContext.setIpAddress(request.getRemoteAddr());
            guestContext.setUserAgent(request.getHeader("User-Agent"));

            // Add guest header for tracking
            response.setHeader("X-Guest-Id", guestContext.getGuestId());
        } else {
            guestContext.setGuest(false);
        }

        return true;
    }

    private String generateGuestId(HttpServletRequest request) {
        // Generate unique guest ID based on IP + User-Agent hash
        String ip = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");
        String raw = ip + ":" + (userAgent != null ? userAgent : "");

        return "guest_" + UUID.nameUUIDFromBytes(raw.getBytes()).toString().substring(0, 8);
    }
}