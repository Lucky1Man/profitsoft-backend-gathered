package org.example.task2restapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
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

@Data
@NoArgsConstructor
@AllArgsConstructor
@DateTimeRangeConstraint
public class ExecutionFactFilterOptionsDto implements ObjectWithDateTimeRanges {
    @Nullable
    @Email
    private String executorEmail;
    @Nullable
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime fromFinishTime;
    @Nullable
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime toFinishTime;
    @Nullable
    @Length(min = 1, max = 500)
    private String description;
    @Nullable
    @PositiveOrZero
    private Integer pageIndex;
    @Nullable
    @Positive
    private Integer pageSize;

    @Override
    @JsonIgnore
    public List<DateTimeRange> getValidatedRanges() {
        return List.of(
                new DateTimeRange(fromFinishTime, toFinishTime, "fromFinishTime", "toFinishTime")
        );
    }

}
