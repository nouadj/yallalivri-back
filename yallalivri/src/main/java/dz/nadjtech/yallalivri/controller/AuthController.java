package dz.nadjtech.yallalivri.controller;

import dz.nadjtech.yallalivri.dto.AuthRequest;
import dz.nadjtech.yallalivri.dto.AuthResponse;
import dz.nadjtech.yallalivri.dto.UserDTO;
import dz.nadjtech.yallalivri.service.UserService;
import dz.nadjtech.yallalivri.service.impl.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<AuthResponse>> login(@RequestBody AuthRequest request) {
        return authService.login(request)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/me")
    public Mono<UserDTO> getCurrentUser(@AuthenticationPrincipal Jwt jwt)  {
        String email = jwt.getClaim("email");
        return userService.findByEmail(email);
    }
}
