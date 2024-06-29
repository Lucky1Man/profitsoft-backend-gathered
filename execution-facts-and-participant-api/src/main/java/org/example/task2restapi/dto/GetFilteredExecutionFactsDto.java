package org.example.task2restapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(setterPrefix = "with")
public class GetFilteredExecutionFactsDto {
    private List<GetExecutionFactDto> executionFacts;
    private Integer totalPages;
}
