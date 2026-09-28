package org.riteshingle.campusgig.AdminResponseDTO;

import lombok.Builder;
import lombok.Data;
import org.riteshingle.campusgig.Enum.TechnicalSupportStatus;

import java.time.LocalDateTime;

@Data
@Builder
public class AdminTechnicalSupportResponseDTO {
    private Long id;
    private AdminUserAndClientResponseDTO userResponseDTO;
    private String subject;
    private String description;
    private TechnicalSupportStatus status;
    private LocalDateTime createdAt;
}
