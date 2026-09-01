package org.riteshingle.campusgig.RequestDTO;

import lombok.Data;

@Data
public class AdminSendMailRequestDTO {
    private String body;
    private String subject;
    private String to;
    private Long id;
}
