package org.riteshingle.campusgig.Specification;

import org.riteshingle.campusgig.Enum.JobApplicationStatus;
import org.riteshingle.campusgig.Model.Job;
import org.riteshingle.campusgig.Model.JobApplication;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class GigSpecification {

    public static Specification<JobApplication> hasGig(Long gigId) {
        return ((root, query, criteriaBuilder) -> {

            if (gigId == null) return criteriaBuilder.conjunction();
            return criteriaBuilder.equal(root.get("gig").get("id"), gigId);
        });
    }

    public static Specification<JobApplication> hasStatus(JobApplicationStatus jobApplicationStatus) {
        return ((root, query, criteriaBuilder) ->
                criteriaBuilder.equal((jobApplicationStatus == null) ? null : root.get("jobApplicationStatus"), jobApplicationStatus));
    }

    public static Specification<JobApplication> budgetGraterThan(BigDecimal minBudget) {
        return ((root, query, criteriaBuilder) ->
                criteriaBuilder.greaterThan((minBudget == null) ? null : root.get("bidAmount"), minBudget));
    }

    public static Specification<JobApplication> budgetLessThan(BigDecimal maxBudget) {
        return ((root, query, criteriaBuilder) ->
                criteriaBuilder.lessThan((maxBudget == null) ? null : root.get("bidAmount"), maxBudget));
    }
}
