package org.riteshingle.campusgig.AdminRequestDTO;

import lombok.Data;

@Data
public class ResetPasswordRequestDTO {
    private String oldPassword;
    private String newPassword;
    private String confirmPassword;
}
