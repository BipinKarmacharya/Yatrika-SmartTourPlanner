package com.yatrika.shared.security;

import lombok.Data;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequestScope
@Data
public class GuestContext {
    private boolean isGuest = false;
    private String guestId;
    private String ipAddress;
    private String userAgent;

    public boolean isAuthenticatedUser() {
        return !isGuest;
    }
}