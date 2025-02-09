package dz.nadjtech.yallalivri.dto;

public class StoreDTO extends UserDTO {

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
