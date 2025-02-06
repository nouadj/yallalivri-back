package dz.nadjtech.yallalivri.entity;

import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;

@Table("couriers")
public class Courier extends User {

    LocalDate dateOfBirth;

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }
}
