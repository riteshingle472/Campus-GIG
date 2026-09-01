package org.riteshingle.campusgig.ResponseDTO;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.riteshingle.campusgig.Enum.ContractStatus;
import org.riteshingle.campusgig.Enum.ProgressStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminContractResponseDTO {
    private Long id;
    private BigDecimal agreementAmount;
    private AdminUserAndClientResponseDTO clientResponseDTO;
    private AdminJobApplicationResponseDTO jobApplicationResponseDTO;
    private ContractStatus contractStatus;
    private ProgressStatus progressStatus;
    private LocalDate expectedDeliveryDate;
    private LocalDateTime createdAt;
}
