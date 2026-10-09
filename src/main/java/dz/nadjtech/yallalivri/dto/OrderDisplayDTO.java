package dz.nadjtech.yallalivri.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderDisplayDTO extends OrderDTO {
    private String storeName;
    private String storeAddress;
    private String courierName;
    private String courierPhone;
    private String storePhone;
}
