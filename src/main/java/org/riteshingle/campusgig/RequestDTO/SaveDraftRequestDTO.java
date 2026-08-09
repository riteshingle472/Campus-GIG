package org.riteshingle.campusgig.RequestDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SaveDraftRequestDTO {
    private String draftId;
    private CreateJobRequestDTO dto;
}
