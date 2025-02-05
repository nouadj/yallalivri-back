package dz.nadjtech.yallalivri.mapper;

import dz.nadjtech.yallalivri.dto.StoreDTO;
import dz.nadjtech.yallalivri.entity.Store;
import org.springframework.stereotype.Component;

@Component
public class StoreMapper {

    public StoreDTO toDTO(Store store) {
        if (store == null) {
            return null;
        }

        StoreDTO dto = new StoreDTO();
        dto.setId(store.getId());
        dto.setName(store.getName());
        dto.setEmail(store.getEmail());
        dto.setRole(store.getRole());
        dto.setPhone(store.getPhone());
        dto.setCreatedAt(store.getCreatedAt());
        dto.setUpdatedAt(store.getUpdatedAt());
        dto.setAddress(store.getAddress());

        return dto;
    }

    public Store toEntity(StoreDTO dto) {
        if (dto == null) {
            return null;
        }

        Store store = new Store();
        store.setId(dto.getId());
        store.setName(dto.getName());
        store.setEmail(dto.getEmail());
        store.setRole(dto.getRole());
        store.setPhone(dto.getPhone());
        store.setCreatedAt(dto.getCreatedAt());
        store.setUpdatedAt(dto.getUpdatedAt());
        store.setAddress(dto.getAddress());

        return store;
    }
}
