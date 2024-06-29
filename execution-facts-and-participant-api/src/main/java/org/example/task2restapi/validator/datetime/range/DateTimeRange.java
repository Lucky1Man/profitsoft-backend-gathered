package org.example.task2restapi.validator.datetime.range;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * This class holds date time range for validation purposes.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class DateTimeRange {
    private LocalDateTime from;
    private LocalDateTime to;
    private String fromFieldName;
    private String toFieldName;

    public DateTimeRange(LocalDateTime from, LocalDateTime to) {
        this(from, to, "fromDateTime", "toDateTime");
    }
}
