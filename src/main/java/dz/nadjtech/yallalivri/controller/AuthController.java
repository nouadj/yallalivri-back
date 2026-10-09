package dz.nadjtech.yallalivri.controller;

import dz.nadjtech.yallalivri.dto.AuthRequest;
import dz.nadjtech.yallalivri.dto.AuthResponse;
import dz.nadjtech.yallalivri.dto.NotificationTokenRequest;
import dz.nadjtech.yallalivri.dto.UserDTO;
import dz.nadjtech.yallalivri.security.CurrentUser;
import dz.nadjtech.yallalivri.service.AuthService;
import dz.nadjtech.yallalivri.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUser(@AuthenticationPrincipal CurrentUser currentUser) {
        return ResponseEntity.ok(userService.getUserById(currentUser.id(), currentUser));
    }

    @PatchMapping("/users/{id}/notification-token")
    public ResponseEntity<UserDTO> updateNotificationToken(
            @PathVariable Long id,
            @Valid @RequestBody NotificationTokenRequest request,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.ok(userService.updateNotificationToken(id, request, currentUser));
    }
}
