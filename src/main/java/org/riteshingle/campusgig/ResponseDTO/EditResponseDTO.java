package org.riteshingle.campusgig.ResponseDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EditResponseDTO {
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
    private String dob;
}
