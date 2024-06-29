package org.example.task2restapi.service;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Main responsibility is to provide unified way of obtaining dates.
 */
public interface DateTimeService {

    /**
     * @return LocalDateTime.now() at utc time zone
     */
    LocalDateTime utcNow();

    Instant instantUtcNow();

}
