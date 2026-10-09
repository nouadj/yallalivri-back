package dz.nadjtech.yallalivri.security;

import dz.nadjtech.yallalivri.enums.UserRole;

public record CurrentUser(
        Long id,
        String email,
        UserRole role
) {
    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }
}
