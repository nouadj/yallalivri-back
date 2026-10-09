package dz.nadjtech.yallalivri.dto;

import dz.nadjtech.yallalivri.enums.StoreType;
import dz.nadjtech.yallalivri.enums.UserRole;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {
    private Long id;
    private String name;
    private UserRole role;
    private String phone;
    private String email;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String address;
    private String notificationToken;
    private Double latitude;
    private Double longitude;
    private LocalDate dateOfBirth;
    private StoreType storeType;
}
