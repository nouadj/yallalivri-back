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
        dto.setAddress(store.getAddress());
        dto.setType(store.getType());

        return dto;
    }

    public Store toEntity(StoreDTO dto) {
        if (dto == null) {
            return null;
        }

        Store store = new Store();
        store.setAddress(dto.getAddress());
        store.setType(dto.getType());

        return store;
    }
}
