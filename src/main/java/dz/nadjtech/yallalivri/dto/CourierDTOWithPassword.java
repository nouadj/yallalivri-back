package dz.nadjtech.yallalivri.dto;

import java.time.LocalDate;

public class CourierDTOWithPassword extends CourierDTO {
    private String password;
    private LocalDate dateOfBirth;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }
}
