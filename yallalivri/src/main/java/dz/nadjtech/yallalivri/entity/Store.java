package dz.nadjtech.yallalivri.entity;

import dz.nadjtech.yallalivri.dto.StoreType;
import org.springframework.data.relational.core.mapping.Table;


@Table("stores")
public class Store extends User {
    private String address;
    private StoreType type;

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public StoreType getType() {
        return type;
    }

    public void setType(StoreType type) {
        this.type = type;
    }
}
