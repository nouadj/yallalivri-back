package dz.nadjtech.yallalivri.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminPasswordUpdateRequest {
    @NotBlank
    @Size(min = 6)
    private String newPassword;
}
