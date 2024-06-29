package org.example.task2restapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.task2restapi.validator.datetime.range.DateTimeRange;
import org.example.task2restapi.validator.datetime.range.DateTimeRangeConstraint;
import org.example.task2restapi.validator.datetime.range.ObjectWithDateTimeRanges;
import org.hibernate.validator.constraints.Length;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@DateTimeRangeConstraint
@Builder(setterPrefix = "with")
public class RecordExecutionFactDto implements ObjectWithDateTimeRanges {

    @NotNull(message = "Execution fact must have executor id")
    private UUID executorId;

    @NotNull(message = "Execution fact must have description")
    @Length(min = 1, max = 500)
    private String description;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Nullable
    private LocalDateTime startTime;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Nullable
    private LocalDateTime finishTime;

    @Override
    @JsonIgnore
    public List<DateTimeRange> getValidatedRanges() {
        return List.of(
                new DateTimeRange(startTime, finishTime, "startTime", "finishTime")
        );
    }
}
