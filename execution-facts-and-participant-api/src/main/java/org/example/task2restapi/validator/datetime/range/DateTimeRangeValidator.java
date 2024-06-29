package org.example.task2restapi.validator.datetime.range;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Validates given date time ranges by ObjectWithDateTimeRanges interface.
 */
public class DateTimeRangeValidator implements ConstraintValidator<DateTimeRangeConstraint, ObjectWithDateTimeRanges> {

    /**
     * <p>Checks if from date is before to date in all given ranges.</p>
     * If some date time range contains null values then that range is ignored.
     * @return true if all ranges are valid, false if at least one is invalid.
     */
    @Override
    public boolean isValid(ObjectWithDateTimeRanges objectWithDateTimeRanges, ConstraintValidatorContext constraintValidatorContext) {
        List<DateTimeRange> validatedRanges = objectWithDateTimeRanges.getValidatedRanges();
        constraintValidatorContext.disableDefaultConstraintViolation();
        return validatedRanges.stream().allMatch(range -> fromDateIsBeforeToDate(range, constraintValidatorContext));
    }

    private boolean fromDateIsBeforeToDate(DateTimeRange range, ConstraintValidatorContext ctx) {
        LocalDateTime from = range.getFrom();
        LocalDateTime to = range.getTo();
        if(from != null && to != null && from.isAfter(to)) {
            ctx.buildConstraintViolationWithTemplate(
                    "%s is after %s".formatted(range.getFromFieldName(), range.getToFieldName())
            ).addConstraintViolation();
            return false;
        }
        return true;
    }
}
