package dz.nadjtech.yallalivri.service.impl;

import dz.nadjtech.yallalivri.dto.AuthRequest;
import dz.nadjtech.yallalivri.dto.AuthResponse;
import dz.nadjtech.yallalivri.security.JwtUtil;
import dz.nadjtech.yallalivri.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserService userService, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public Mono<AuthResponse> login(AuthRequest authRequest) {
        return userService.findByEmailWithPassword(authRequest.getEmail())
                .flatMap(user -> {
                    if (passwordEncoder.matches(authRequest.getPassword().trim(), user.getPassword().trim())) {
                        String token = JwtUtil.generateToken(user);
                        return Mono.just(new AuthResponse(token));
                    } else {
                        return Mono.error(new RuntimeException("Identifiants incorrects"));
                    }
                })
                .switchIfEmpty(Mono.error(new RuntimeException("Utilisateur non trouvé")));
    }


}
