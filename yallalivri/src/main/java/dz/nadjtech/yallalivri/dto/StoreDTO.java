package dz.nadjtech.yallalivri.dto;

public class StoreDTO extends UserDTO {
    private String address;

    @Override
    public String getAddress() {
        return address;
    }

    @Override
    public void setAddress(String address) {
        this.address = address;
    }
}
