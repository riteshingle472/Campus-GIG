package org.riteshingle.campusgig.Specification;

import org.riteshingle.campusgig.Enum.JobStatus;
import org.riteshingle.campusgig.Model.Job;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Component
public class JobSpecification {
    public static Specification<Job> hasClient(Long clientId){
        return ((root, query, criteriaBuilder) ->
                criteriaBuilder.equal((clientId == null) ? null : root.get("job").get("id"),clientId));
    }

    public static Specification<Job> hasStatus(JobStatus status){
        return ((root, query, criteriaBuilder) ->
                criteriaBuilder.equal((status == null) ? null : root.get("job").get("jobStatus"),status));
    }
}
