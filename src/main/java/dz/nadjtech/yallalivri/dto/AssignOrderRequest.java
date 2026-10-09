package dz.nadjtech.yallalivri.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AssignOrderRequest {
    @NotNull
    private Long courierId;
}
