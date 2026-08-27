package org.riteshingle.campusgig.ResponseDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.riteshingle.campusgig.Enum.AvailabilityStatus;
import org.riteshingle.campusgig.Enum.JobCategory;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GigResponseDTO {
    private String gigFirstName;
    private String gigLastName;
    private String gigEmail;
    private String gigPhoneNumber;
    private String college;
    private String department;
    private Integer semester;
    private String description;
    private JobCategory title;
    private AvailabilityStatus availabilityStatus;
    private List<String> gigSkills;
}