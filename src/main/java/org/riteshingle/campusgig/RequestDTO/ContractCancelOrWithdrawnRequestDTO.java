package org.riteshingle.campusgig.RequestDTO;

import jakarta.persistence.Column;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ContractCancelOrWithdrawnRequestDTO {

    @Column(nullable = false)
    private String reason;

    @Column(nullable = false)
    @Size(min = 10,max = 1000,message = "remark must be between 10 to 1000 character...")
    private String remark;
}
