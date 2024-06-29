package org.example.task2restapi.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.task2restapi.dto.ExecutionFactFilterOptionsDto;
import org.example.task2restapi.dto.ExecutionFactUploadResultDto;
import org.example.task2restapi.dto.GetDetailedExecutionFactDto;
import org.example.task2restapi.dto.GetExecutionFactDto;
import org.example.task2restapi.dto.GetFilteredExecutionFactsDto;
import org.example.task2restapi.dto.RecordExecutionFactDto;
import org.example.task2restapi.dto.UpdateExecutionFactDto;
import org.example.task2restapi.service.ExecutionFactService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/execution-facts")
public class ExecutionFactController {

    private final ExecutionFactService factService;

    @PostMapping
    @Operation(
            description = "Records given execution fact. If start time is not given then current time by UTC is assigned."
    )
    @ResponseStatus(HttpStatus.CREATED)
    @ApiResponse(
            responseCode = "201",
            description = "Returns id of execution fact that was recorded.",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(pattern = """
                            {
                                "id": "0cecc52a-2342-4dfd-83e6-dd6a38a3c119"
                            }
                            """)
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "Returns message containing all validation errors.",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ExceptionResponse.class)
            )
    )
    public ResponseEntity<Map<String, UUID>> recordExecutionFact(@RequestBody RecordExecutionFactDto factDto) {
        log.debug("recording execution fact: {}", factDto);
        UUID id = factService.recordExecutionFact(factDto);
        log.debug("recorded execution fact, assigned id = {}", id);
        return ResponseEntity.status(HttpStatus.CREATED.value())
                .body(Map.of("id", id));
    }

    @GetMapping("/{id}")
    @Operation(description = "Returns detailed version of execution fact by given id.")
    @ApiResponse(
            responseCode = "200",
            description = "Retrieved",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = GetDetailedExecutionFactDto.class)
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "Means that execution fact with given id was not found",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ExceptionResponse.class)
            )
    )
    public GetDetailedExecutionFactDto getById(@PathVariable UUID id) {
        log.debug("getting execution fact by id: {}", id);
        GetDetailedExecutionFactDto byId = factService.getById(id);
        log.debug("got execution fact: {}", byId);
        return byId;
    }

    @PutMapping("/{id}")
    @Operation(
            description = "Updates execution fact with given id with data from UpdateExecutionFactDto." +
                    " If UpdateExecutionFactDto has null fields then that specific field will be ignored"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Means that execution fact was updated and all given parameters were changed"
    )
    @ApiResponse(
            responseCode = "400",
            description = "Given data is invalid.",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ExceptionResponse.class)
            )
    )
    public void updateExecutionFact(@PathVariable UUID id, @RequestBody UpdateExecutionFactDto factDto) {
        log.debug("updating execution fact with id {} with data {}", id, factDto);
        factService.updateExecutionFact(id, factDto);
        log.debug("updated  execution fact with id {}", id);
    }

    @DeleteMapping("/{id}")
    @Operation(
            description = "Deletes execution fact with given id, if given id does not exist it ignores it."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Means that execution fact was deleted"
    )
    public void deleteExecutionFact(@PathVariable UUID id) {
        log.debug("deleting execution fact with id {}", id);
        factService.deleteById(id);
        log.debug("deleted execution fact with id {}", id);
    }

    @PostMapping(path = "/_list")
    @Operation(description = "Returns execution facts based on given filter. Pagination is zero based. " +
                             "If fromFinishTime or toFinishTime is null then both of them are ignored. " +
                             "Default values: pageIndex = 0, pageSize = 50")
    @ApiResponse(
            responseCode = "200",
            description = "Retrieved",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    array = @ArraySchema(
                            schema = @Schema(implementation = GetExecutionFactDto.class)
                    )
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "Given filter is invalid, detailed message provided",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = GetFilteredExecutionFactsDto.class)
            )
    )
    public GetFilteredExecutionFactsDto getFiltered(@RequestBody ExecutionFactFilterOptionsDto factFilterOptionsDto) {
        log.debug("getting filtered execution facts by {}", factFilterOptionsDto);
        GetFilteredExecutionFactsDto result = factService.findAll(factFilterOptionsDto);
        log.debug("got filtered execution facts {}", result);
        return result;
    }

    @PostMapping(value = "/_report", produces = "application/csv")
    @Operation(description = "Generates csv file containing all based on given filter. Pagination is zero based. " +
                             "If fromFinishTime or toFinishTime is null then both of them are ignored. " +
                             "Default values: pageIndex = 0, pageSize = 50")
    @ApiResponse(
            responseCode = "200",
            description = "Retrieved",
            content = @Content(
                    mediaType = "application/csv"
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "Given filter is invalid, detailed message provided",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    array = @ArraySchema(
                            schema = @Schema(implementation = ExceptionResponse.class)
                    )
            )
    )
    public ResponseEntity<Resource> generateReport(@RequestBody ExecutionFactFilterOptionsDto factFilterOptionsDto) {
        log.debug("getting execution files in csv format by filter {}", factFilterOptionsDto);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=execution-facts.csv")
                .contentType(MediaType.parseMediaType("application/csv"))
                .body(new InputStreamResource(factService.generateCsvReport(factFilterOptionsDto)));
    }

    @PostMapping("/upload")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(description = "Writes all valid execution facts from given json file to database." +
            " It returns count of added execution facts and list of declined objects and info what is wrong with them.")
    @ApiResponse(
            responseCode = "201",
            description = "Valid facts where saved.",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ExecutionFactUploadResultDto.class)
            )
    )
    public ExecutionFactUploadResultDto uploadFromFile(@RequestParam("file") MultipartFile multipart) {
        log.debug("uploading execution facts from file");
        ExecutionFactUploadResultDto result = factService.uploadFromFile(multipart);
        log.debug("upload result: {}", result);
        return result;
    }

}
