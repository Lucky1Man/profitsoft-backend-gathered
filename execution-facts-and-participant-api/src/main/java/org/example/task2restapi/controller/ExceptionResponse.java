package org.example.task2restapi.controller;

import lombok.Builder;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@Data
@Builder(setterPrefix = "with")
public class ExceptionResponse {

    private final String message;

    private final HttpStatus httpStatus;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private final LocalDateTime date;

}
