package dz.nadjtech.yallalivri.entity;

import org.springframework.data.relational.core.mapping.Table;


@Table("stores")
public class Store extends User {
    private String address;

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
