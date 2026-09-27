package org.riteshingle.campusgig.RequestDTO;

import lombok.Data;

@Data
public class AdminAuthDTO {
    private String email;
    private String password;
    private String fullName;
    private String contactNo;
}
