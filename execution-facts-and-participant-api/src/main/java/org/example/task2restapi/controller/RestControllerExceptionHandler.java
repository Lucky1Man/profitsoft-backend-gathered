package org.example.task2restapi.controller;

import lombok.Generated;
import lombok.RequiredArgsConstructor;
import org.example.task2restapi.service.DateTimeService;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;


@RestControllerAdvice
@Generated
@RequiredArgsConstructor
public class RestControllerExceptionHandler {

    private final DateTimeService dateTimeService;

    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ExceptionResponse handleConstraintViolationException(jakarta.validation.ConstraintViolationException e) {
        return ExceptionResponse.builder()
                .withMessage(e.getMessage())
                .withHttpStatus(HttpStatus.BAD_REQUEST)
                .withDate(dateTimeService.utcNow())
                .build();
    }

    @ExceptionHandler(org.hibernate.exception.ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ExceptionResponse handleHibernateConstraintViolationException(
            org.hibernate.exception.ConstraintViolationException e) {
        return ExceptionResponse.builder()
                .withMessage(e.getMessage())
                .withHttpStatus(HttpStatus.BAD_REQUEST)
                .withDate(dateTimeService.utcNow())
                .build();
    }


    @ExceptionHandler(ConversionFailedException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ExceptionResponse handleConversionFailedException(
            ConversionFailedException e) {
        return ExceptionResponse.builder()
                .withMessage(e.getMessage())
                .withHttpStatus(HttpStatus.BAD_REQUEST)
                .withDate(dateTimeService.utcNow())
                .build();
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ExceptionResponse handleHttpMessageNotReadableException(
            HttpMessageNotReadableException e) {
        return ExceptionResponse.builder()
                .withMessage(e.getMessage())
                .withHttpStatus(HttpStatus.BAD_REQUEST)
                .withDate(dateTimeService.utcNow())
                .build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ExceptionResponse handleIllegalArgumentExceptionException(
            IllegalArgumentException e) {
        return ExceptionResponse.builder()
                .withMessage(e.getMessage())
                .withHttpStatus(HttpStatus.BAD_REQUEST)
                .withDate(dateTimeService.utcNow())
                .build();
    }

}
