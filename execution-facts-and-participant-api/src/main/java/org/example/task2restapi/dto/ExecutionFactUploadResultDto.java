package org.example.task2restapi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionFactUploadResultDto {
    private Integer importedCount;
    private Integer failedCount;
    private List<RecordFactToItsValidationExceptions> objectToItsErrors;
}
