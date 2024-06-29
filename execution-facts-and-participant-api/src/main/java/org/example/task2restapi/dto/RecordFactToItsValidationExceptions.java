package org.example.task2restapi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.task2restapi.controller.ExceptionResponse;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecordFactToItsValidationExceptions {
    private RecordExecutionFactDto factDto;
    private ExceptionResponse errors;
}
