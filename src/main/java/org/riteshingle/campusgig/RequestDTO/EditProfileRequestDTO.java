package org.riteshingle.campusgig.RequestDTO;

import lombok.Data;

@Data
public class EditProfileRequestDTO {
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String college;
    private String department;
    private Integer semester;
    private String profileImage;
    private String shortBio;
    private String AvailabilityStatus;
}
