package org.riteshingle.campusgig.RequestDTO;

import lombok.Data;

@Data
public class AdminAuthDTO {
    private String fullName;
    private String email;
    private String password;
}
