package dz.nadjtech.yallalivri.mapper;

import dz.nadjtech.yallalivri.dto.CourierDTO;
import dz.nadjtech.yallalivri.entity.Courier;
import org.springframework.stereotype.Component;

@Component
public class CourierMapper {

    public CourierDTO toDTO(Courier courier) {
        if (courier == null) {
            return null;
        }

        CourierDTO dto = new CourierDTO();
        dto.setId(courier.getId());
        dto.setName(courier.getName());
        dto.setEmail(courier.getEmail());
        dto.setRole(courier.getRole());
        dto.setPhone(courier.getPhone());
        dto.setCreatedAt(courier.getCreatedAt());
        dto.setUpdatedAt(courier.getUpdatedAt());
        dto.setAge(30);
       // dto.setDateOfBirth(courier.getDateOfBirth());

        return dto;
    }

    public Courier toEntity(CourierDTO dto) {
        if (dto == null) {
            return null;
        }

        Courier courier = new Courier();
        courier.setId(dto.getId());
        courier.setName(dto.getName());
        courier.setEmail(dto.getEmail());
        courier.setRole(dto.getRole());
        courier.setPhone(dto.getPhone());
        courier.setCreatedAt(dto.getCreatedAt());
        courier.setUpdatedAt(dto.getUpdatedAt());
        //courier.setDateOfBirth(dto.getDateOfBirth());

        return courier;
    }
}
