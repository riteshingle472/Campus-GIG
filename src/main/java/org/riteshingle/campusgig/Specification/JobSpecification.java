package org.riteshingle.campusgig.Specification;

import jakarta.persistence.criteria.Predicate;
import org.riteshingle.campusgig.Enum.ExperienceLevel;
import org.riteshingle.campusgig.Enum.JobCategory;
import org.riteshingle.campusgig.Enum.JobStatus;
import org.riteshingle.campusgig.Model.Job;
import org.springframework.beans.factory.BeanRegistry;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class JobSpecification {
    public static Specification<Job> hasClient(Long clientId){
        return ((root, query, criteriaBuilder) ->
                criteriaBuilder.equal((clientId == null) ? null : root.get("job").get("id"),clientId));
    }

    public static Specification<Job> hasStatus(JobStatus status){
        return ((root, query, criteriaBuilder) ->
                criteriaBuilder.equal((status == null) ? null : root.get("jobStatus"),status));
    }

    public static Specification<Job> minBudget(BigDecimal min){
        return ((root, query, criteriaBuilder) ->
                criteriaBuilder.greaterThanOrEqualTo((min == null) ? null : root.get("budget"),min));
    }

    public static Specification<Job> hasCategory(JobCategory category){
        return ((root, query, criteriaBuilder) ->
                criteriaBuilder.equal(category == null ? null : root.get("category"),category));
    }

    public static Specification<Job> hasMinDeadline(LocalDate deadline) {
        return (root, query, criteriaBuilder) ->
                deadline == null ? criteriaBuilder.conjunction() : criteriaBuilder.greaterThanOrEqualTo(root.get("deadline"),deadline);
    }

    public static Specification<Job> hasMaxDeadline(LocalDate deadline) {
        return (root, query, criteriaBuilder) ->
                deadline == null ? criteriaBuilder.conjunction() : criteriaBuilder.lessThanOrEqualTo(root.get("deadline"), deadline);
    }
    public static Specification<Job> maxBudget(BigDecimal max){
        return ((root, query, criteriaBuilder) ->
                criteriaBuilder.lessThanOrEqualTo((max == null) ? null : root.get("budget"),max));
    }

    public static Specification<Job> experienceLevel(ExperienceLevel experienceLevel){
        return ((root, query, criteriaBuilder) ->
                criteriaBuilder.equal((experienceLevel == null) ? null : root.get("experienceLevel"),experienceLevel));
    }

//    public static Specification<Job> hasSearch(String search) {
//        return (root, query, cb) -> {
//            if (search == null || search.isBlank())
//                return cb.conjunction();
//
//            List<Predicate> predicates = new ArrayList<>();
//            String[] words = search.toLowerCase().trim().split("\\s+");
//
//            for(String word : words) {
//                if(word.length() < 2)
//                    continue;
//
//                String keyword = "%" + word + "%";
//
//                Predicate titleMatch = cb.like(cb.lower(root.get("title")),keyword);
//                Predicate descriptionMatch = cb.like(cb.lower(root.get("description")),keyword);
//
//                predicates.add(cb.or(titleMatch, descriptionMatch));
//            }
//            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
//        };
//    }

    public static Specification<Job> hasSearch(String search) {
        return (root, query, cb) -> {
            if (search == null || search.isBlank()) {
                return cb.conjunction();
            }

            String keyword = "%" + search.trim().toLowerCase() + "%";

            return cb.or(
                    cb.like(cb.lower(root.get("title")), keyword),
                    cb.like(cb.lower(root.get("description")), keyword)
            );
        };
    }
}
