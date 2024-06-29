package org.example.task2restapi.service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.example.task2restapi.dto.ExecutionFactFilterOptionsDto;
import org.example.task2restapi.dto.ExecutionFactUploadResultDto;
import org.example.task2restapi.dto.GetDetailedExecutionFactDto;
import org.example.task2restapi.dto.GetFilteredExecutionFactsDto;
import org.example.task2restapi.dto.RecordExecutionFactDto;
import org.example.task2restapi.dto.UpdateExecutionFactDto;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.UncheckedIOException;
import java.util.UUID;

/**
 * Provides api for ExecutionFact manipulation.
 */
@Validated
public interface ExecutionFactService {
    /**
     * @param factDto fact to be saved
     * @return id of saved fact
     * @throws IllegalArgumentException given RecordExecutionFactDto is invalid
     * @throws jakarta.validation.ConstraintViolationException given RecordExecutionFactDto is invalid
     */
    UUID recordExecutionFact(@NotNull @Valid RecordExecutionFactDto factDto);

    /**
     * @param id id of needed fact
     * @return detailed information about fact
     * @throws IllegalArgumentException fact by given id was not found
     */
    GetDetailedExecutionFactDto getById(@NotNull UUID id);

    /**
     * @param id id of fact to be updated
     * @param factDto data to update fact with, all null fields are ignored
     * @throws IllegalArgumentException given invalid data
     * @throws jakarta.validation.ConstraintViolationException given invalid data
     */
    void updateExecutionFact(@NotNull UUID id, @NotNull @Valid UpdateExecutionFactDto factDto);

    /**
     * Ignores non-existing ids.
     * @param id id of fact to be deleted
     */
    void deleteById(@NotNull UUID id);

    /**
     *
     * @param factFilterOptionsDto filter parameters
     * @return facts by filter
     * @throws IllegalArgumentException given invalid filter
     * @throws jakarta.validation.ConstraintViolationException given invalid filter
     */
    GetFilteredExecutionFactsDto findAll(@NotNull @Valid ExecutionFactFilterOptionsDto factFilterOptionsDto);

    /**
     * @param factFilterOptionsDto filter parameters
     * @return csv file containing all facts by filter
     * @throws IllegalArgumentException given invalid filter
     * @throws jakarta.validation.ConstraintViolationException given invalid filter
     */
    ByteArrayInputStream generateCsvReport(@NotNull @Valid ExecutionFactFilterOptionsDto factFilterOptionsDto);

    /**
     * @param multipart file from which to upload
     * @return result of upload
     * @throws UncheckedIOException given file contains invalid json
     */
    ExecutionFactUploadResultDto uploadFromFile(@NotNull MultipartFile multipart);
}
