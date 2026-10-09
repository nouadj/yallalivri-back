package dz.nadjtech.yallalivri.dto;

import dz.nadjtech.yallalivri.enums.OrderStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDTO {
    private Long id;

    @NotNull
    private Long storeId;

    private Long courierId;

    @NotBlank
    @jakarta.validation.constraints.Size(max = 120)
    private String customerName;

    @NotBlank
    @jakarta.validation.constraints.Size(max = 30)
    private String customerPhone;

    @NotBlank
    private String customerAddress;

    private OrderStatus status;
    @NotNull
    @jakarta.validation.constraints.PositiveOrZero
    private Double amount;
    @NotNull
    @jakarta.validation.constraints.PositiveOrZero
    private Double deliveryFee;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Double storeLatitude;
    private Double storeLongitude;
}
