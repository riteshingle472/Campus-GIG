package org.riteshingle.campusgig.RequestDTO;

import lombok.Data;

import java.util.List;

@Data
public class AddUserSkillsRequestDTO {
    private List<Long> skillsId;
}
