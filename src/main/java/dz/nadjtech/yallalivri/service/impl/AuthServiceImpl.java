package dz.nadjtech.yallalivri.service.impl;

import dz.nadjtech.yallalivri.dto.AuthRequest;
import dz.nadjtech.yallalivri.dto.AuthResponse;
import dz.nadjtech.yallalivri.entity.User;
import dz.nadjtech.yallalivri.repository.UserRepository;
import dz.nadjtech.yallalivri.security.JwtUtil;
import dz.nadjtech.yallalivri.service.AuthService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public AuthResponse login(AuthRequest authRequest) {
        User user = userRepository.findByEmail(authRequest.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new BadCredentialsException("Identifiants incorrects"));

        if (!passwordEncoder.matches(authRequest.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Identifiants incorrects");
        }

        return AuthResponse.builder()
                .token(jwtUtil.generateToken(user))
                .userId(user.getId())
                .role(user.getRole())
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }
}
