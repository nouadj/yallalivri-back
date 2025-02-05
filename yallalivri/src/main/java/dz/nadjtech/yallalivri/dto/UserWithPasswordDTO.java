package dz.nadjtech.yallalivri.dto;

public class UserWithPasswordDTO extends UserDTO {
    String password;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
