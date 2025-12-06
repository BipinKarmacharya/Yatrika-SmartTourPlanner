package com.yatrika.shared.security;

import com.yatrika.user.domain.UserRole;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class RolePermissionService {

    public boolean isGuest() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ||
                !authentication.isAuthenticated() ||
                authentication.getPrincipal().equals("anonymousUser");
    }

    public boolean isUser() {
        return hasRole(UserRole.USER);
    }

    public boolean isAdmin() {
        return hasRole(UserRole.ADMIN);
    }

    public boolean hasRole(UserRole role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role.name()));
    }

    public UserRole getCurrentUserRole() {
        if (isGuest()) return UserRole.GUEST;
        if (isAdmin()) return UserRole.ADMIN;
        if (isUser()) return UserRole.USER;
        return UserRole.GUEST;
    }

    //Permission checks
    public boolean canCreateItinerary() {
        return isUser();    // Only USERS can create itineraries
    }

    public boolean canModifyDestination() {
        return isAdmin();
    }

    public boolean canWriteReview() {
        return isUser();
    }

    public boolean canLikeContent() {
        return isUser();
    }

    public boolean canBookmark() {
        return isUser();
    }

    public boolean canViewAdminDashboard() {
        return isAdmin();
    }
}
