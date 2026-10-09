package dz.nadjtech.yallalivri.dto;

import dz.nadjtech.yallalivri.enums.StoreType;
import dz.nadjtech.yallalivri.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTOWithPassword {
    private Long id;

    @NotBlank
    private String name;

    @NotNull
    private UserRole role;

    private String phone;

    @Email
    @NotBlank
    private String email;

    @Size(min = 6)
    @NotBlank
    private String password;

    private String address;
    private String notificationToken;
    private Double latitude;
    private Double longitude;
    private LocalDate dateOfBirth;
    private StoreType storeType;
}
