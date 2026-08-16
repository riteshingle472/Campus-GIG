package org.riteshingle.campusgig.Model;

import io.jsonwebtoken.Identifiable;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "tbl_user_skills",uniqueConstraints = {@UniqueConstraint(columnNames = {"gig_id", "skill_id"})})
public class UserSkills {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gig_id")
    private GIG gig;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id")
    private Skills skill;

    public UserSkills(GIG gig, Skills skill) {
        this.skill = skill;
        this.gig= gig;
    }
}
