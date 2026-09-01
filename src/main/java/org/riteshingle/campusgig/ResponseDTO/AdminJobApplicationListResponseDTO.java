package org.riteshingle.campusgig.ResponseDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AdminJobApplicationListResponseDTO {
    private Long id;
    private Long clientId;
    private Long applicantId;
    private String clientName;
    private String clientEmail;
    private String applicantName;
    private String applicantEmail;
    private BigDecimal proposedAmount;
    private String coverLetter;
    private String status;
    private LocalDateTime appliedAt;
}
