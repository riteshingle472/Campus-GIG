package org.riteshingle.campusgig.ResponseDTO;

import lombok.Builder;
import lombok.Data;
import org.riteshingle.campusgig.Enum.TechnicalSupportStatus;

import java.time.LocalDateTime;

@Data
@Builder
public class TechnicalSupportResponseDTO {
    private Long id;
    private String subject;
    private String description;
    private TechnicalSupportStatus status;
    private LocalDateTime createdAt;
}
