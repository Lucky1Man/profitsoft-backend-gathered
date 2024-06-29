package org.example.task2restapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
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
public class UpdateExecutionFactDto implements ObjectWithDateTimeRanges {
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Nullable
    private LocalDateTime startTime;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Nullable
    private LocalDateTime finishTime;

    @Length(min = 1, max = 500)
    @Nullable
    private String description;

    @Nullable
    private UUID executorId;

    @Override
    @JsonIgnore
    public List<DateTimeRange> getValidatedRanges() {
        return List.of(
                new DateTimeRange(startTime, finishTime, "startTime", "finishTime")
        );
    }
}
