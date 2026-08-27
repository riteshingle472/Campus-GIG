package org.riteshingle.campusgig.RequestDTO;

import lombok.Data;

import java.time.LocalDate;

@Data
public class EditProfileRequestDTO {
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String profileImage;
    private LocalDate dob;
}
