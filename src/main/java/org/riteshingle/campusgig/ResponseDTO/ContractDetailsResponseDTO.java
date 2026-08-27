package org.riteshingle.campusgig.ResponseDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.riteshingle.campusgig.Enum.ContractStatus;
import org.riteshingle.campusgig.Enum.ProgressStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ContractDetailsResponseDTO {
    private Long contractId;
    private Long conversationId;
    private String gigName;
    private String client;
    private String jobTitle;
    private BigDecimal agreementAmount;
    private LocalDate deadline;
    private ContractStatus status;
    private ProgressStatus progressStatus;
}
