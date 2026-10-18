package org.riteshingle.campusgig.ResponseDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.riteshingle.campusgig.Enum.TechnicalSupportStatus;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TechnicalSupportResponseDTO {
    private Long id;
    private String subject;
    private String description;
    private TechnicalSupportStatus status;
    private LocalDateTime createdAt;
}
