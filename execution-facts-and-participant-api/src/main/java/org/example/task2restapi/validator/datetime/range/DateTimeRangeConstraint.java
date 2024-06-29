package org.example.task2restapi.validator.datetime.range;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = DateTimeRangeValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface DateTimeRangeConstraint {
    String message() default "From date time is after to date time";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
