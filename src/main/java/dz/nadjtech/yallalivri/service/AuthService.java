package dz.nadjtech.yallalivri.service;

import dz.nadjtech.yallalivri.dto.AuthRequest;
import dz.nadjtech.yallalivri.dto.AuthResponse;

public interface AuthService {
    AuthResponse login(AuthRequest authRequest);
}
