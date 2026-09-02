package org.riteshingle.campusgig.ResponseDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.riteshingle.campusgig.Enum.AvailabilityStatus;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminGigResponseDTO {
    private Long id;
    private String title;
    private String college;
    private Integer semester;
    private String category;
    private String department;
    private String description;
    private AvailabilityStatus availabilityStatus;
    private AdminUserAndClientResponseDTO owner;
    private List<String> skills;
    private LocalDateTime createdAt;
}
