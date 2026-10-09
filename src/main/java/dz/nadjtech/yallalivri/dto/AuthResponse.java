package dz.nadjtech.yallalivri.dto;

import dz.nadjtech.yallalivri.enums.UserRole;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    private String token;
    private Long userId;
    private UserRole role;
    private String name;
    private String email;
}
