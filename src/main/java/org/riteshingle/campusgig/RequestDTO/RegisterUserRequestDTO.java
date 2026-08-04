package org.riteshingle.campusgig.RequestDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@Data
@NoArgsConstructor
public class RegisterUserRequestDTO {
    private String firstName;
    private String lastName;
    private String email;
    private String password;
}
