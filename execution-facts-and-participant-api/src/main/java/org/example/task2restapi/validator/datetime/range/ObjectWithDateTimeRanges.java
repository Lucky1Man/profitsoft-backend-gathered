package org.example.task2restapi.validator.datetime.range;

import java.util.List;

/**
 * Interface represents object that can contain multiple date time ranges that need to be validated.
 */
public interface ObjectWithDateTimeRanges {
    List<DateTimeRange> getValidatedRanges();
}
