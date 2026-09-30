package org.riteshingle.campusgig.AdminResponseDTO;

import lombok.Builder;
import lombok.Data;
import org.riteshingle.campusgig.Enum.AdminAccessStatus;
import org.riteshingle.campusgig.Enum.AdminStatus;

import java.time.LocalDateTime;

@Data
@Builder
public class AdminResponseDTO {
    private Long id;
    private String fullName;
    private String email;
    private String contactNumber;
    private AdminAccessStatus accessStatus;
    private AdminStatus status;
    private LocalDateTime createdAt;
}
