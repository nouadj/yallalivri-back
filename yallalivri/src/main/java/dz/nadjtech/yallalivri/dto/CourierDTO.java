package dz.nadjtech.yallalivri.dto;

import java.time.LocalDateTime;

public class CourierDTO extends UserDTO {
    private LocalDateTime dateOfBirth;

    public LocalDateTime getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDateTime dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }
}
