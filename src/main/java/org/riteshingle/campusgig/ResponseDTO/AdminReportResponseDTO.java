package org.riteshingle.campusgig.ResponseDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.riteshingle.campusgig.Enum.ActionInitiatedBy;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminReportResponseDTO{
   private Long id;
   private String reason;
   private String description;
   private String reportStatus;
   private String adminRemark;
   private ActionInitiatedBy actionInitiatedBy;
   private AdminContractResponseDTO contractResponseDTO;
   private LocalDateTime createdAt;
   private LocalDateTime resolvedAt;
}
