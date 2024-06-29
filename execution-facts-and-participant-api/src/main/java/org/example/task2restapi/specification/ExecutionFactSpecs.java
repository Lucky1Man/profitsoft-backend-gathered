package org.example.task2restapi.specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.extern.slf4j.Slf4j;
import org.example.task2restapi.dto.ExecutionFactFilterOptionsDto;
import org.example.task2restapi.entity.ExecutionFact;
import org.example.task2restapi.entity.ExecutionFact_;
import org.example.task2restapi.entity.Participant;
import org.example.task2restapi.entity.Participant_;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

/**
 * This class allows to create dynamic queries for ExecutionFact search.
 */
@Slf4j
@Component
public class ExecutionFactSpecs {

    /**
     * @param optionsDto filter
     * @return specification that represents given filter
     */
    public Specification<ExecutionFact> byFilterDto(ExecutionFactFilterOptionsDto optionsDto) {
        log.debug("generating specification for ExecutionFactFilterOptionsDto {}", optionsDto);
        return (root, query, builder) -> {
            Predicate resultQuery = builder.conjunction();
            resultQuery = addEmailCriteria(optionsDto, root, builder, resultQuery);
            resultQuery = addDescriptionCriteria(optionsDto, root, builder, resultQuery);
            resultQuery = addFinishTimeCriteria(optionsDto, root, builder, resultQuery);
            return resultQuery;
        };
    }

    private Predicate addFinishTimeCriteria(ExecutionFactFilterOptionsDto optionsDto,
                                                   Root<ExecutionFact> root,
                                                   CriteriaBuilder builder,
                                                   Predicate resultQuery) {
        if(optionsDto.getFromFinishTime() != null && optionsDto.getToFinishTime() != null) {
            resultQuery = builder.and(
                    resultQuery,
                    builder.and(
                            builder.greaterThanOrEqualTo(root.get(ExecutionFact_.finishTime), optionsDto.getFromFinishTime()),
                            builder.lessThanOrEqualTo(root.get(ExecutionFact_.finishTime), optionsDto.getToFinishTime())
                    )
            );
            log.debug("added finish time criteria");
        }
        return resultQuery;
    }

    private Predicate addDescriptionCriteria(ExecutionFactFilterOptionsDto optionsDto,
                                                    Root<ExecutionFact> root,
                                                    CriteriaBuilder builder,
                                                    Predicate resultQuery) {
        if(optionsDto.getDescription() != null) {
            resultQuery = builder.and(
                    resultQuery,
                    builder.equal(root.get(ExecutionFact_.description), optionsDto.getDescription())
            );
            log.debug("added description criteria");
        }
        return resultQuery;
    }

    private Predicate addEmailCriteria(ExecutionFactFilterOptionsDto optionsDto,
                                              Root<ExecutionFact> root,
                                              CriteriaBuilder builder,
                                              Predicate resultQuery) {
        if(optionsDto.getExecutorEmail() != null) {
            Join<ExecutionFact, Participant> join = root.join(ExecutionFact_.executor);
            resultQuery = builder.and(
                    resultQuery,
                    builder.equal(join.get(Participant_.email), optionsDto.getExecutorEmail())
            );
            log.debug("added email criteria");
        }
        return resultQuery;
    }

}
