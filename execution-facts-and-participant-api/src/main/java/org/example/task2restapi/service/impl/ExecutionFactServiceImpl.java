package org.example.task2restapi.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.example.task2restapi.controller.ExceptionResponse;
import org.example.task2restapi.dto.ExecutionFactFilterOptionsDto;
import org.example.task2restapi.dto.ExecutionFactUploadResultDto;
import org.example.task2restapi.dto.GetDetailedExecutionFactDto;
import org.example.task2restapi.dto.GetExecutionFactDto;
import org.example.task2restapi.dto.GetFilteredExecutionFactsDto;
import org.example.task2restapi.dto.RecordExecutionFactDto;
import org.example.task2restapi.dto.RecordFactToItsValidationExceptions;
import org.example.task2restapi.dto.UpdateExecutionFactDto;
import org.example.task2restapi.entity.ExecutionFact;
import org.example.task2restapi.entity.Participant;
import org.example.task2restapi.repository.ExecutionFactRepository;
import org.example.task2restapi.repository.ParticipantRepository;
import org.example.task2restapi.service.DateTimeService;
import org.example.task2restapi.service.ExecutionFactService;
import org.example.task2restapi.specification.ExecutionFactSpecs;
import org.modelmapper.MappingException;
import org.modelmapper.ModelMapper;
import org.modelmapper.spi.MappingContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@Transactional
public class ExecutionFactServiceImpl implements ExecutionFactService {

    private final ExecutionFactRepository factRepository;

    private final ParticipantRepository participantRepository;

    private final ModelMapper modelMapper;

    private final DateTimeService dateTimeService;

    private final Integer getFactsMaxPageSize;

    private final ExecutionFactSpecs executionFactSpecs;

    private final Validator validator;

    private final ObjectMapper objectMapper;

    public ExecutionFactServiceImpl(ExecutionFactRepository factRepository,
                                    ParticipantRepository participantRepository,
                                    ModelMapper modelMapper,
                                    DateTimeService dateTimeService,
                                    @Qualifier("executionFactsMaxPageSize")
                                    Integer getFactsMaxPageSize,
                                    ExecutionFactSpecs executionFactSpecs,
                                    Validator validator, ObjectMapper objectMapper) {
        this.factRepository = factRepository;
        this.participantRepository = participantRepository;
        this.modelMapper = modelMapper;
        this.dateTimeService = dateTimeService;
        this.getFactsMaxPageSize = getFactsMaxPageSize;
        this.executionFactSpecs = executionFactSpecs;
        this.validator = validator;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    private void configureModelMapper() {
        log.debug("configuring model mapper type map RecordExecutionFactDto=>ExecutionFact ");
        modelMapper.emptyTypeMap(RecordExecutionFactDto.class, ExecutionFact.class).addMappings(mapping -> {
            mapping.skip(ExecutionFact::setId);
            mapping.using((MappingContext<UUID, Participant> ctx) -> {
                        UUID id = ctx.getSource();
                        if (id == null) {
                            return null;
                        } else {
                            return getRawParticipantOrThrow(id);
                        }
                    }
            ).map(RecordExecutionFactDto::getExecutorId, ExecutionFact::setExecutor);
            mapping.using((MappingContext<LocalDateTime, LocalDateTime> ctx) -> {
                if (ctx.getSource() == null) {
                    return dateTimeService.utcNow();
                }
                return ctx.getSource();
            }).map(
                    RecordExecutionFactDto::getStartTime,
                    ExecutionFact::setStartTime
            );
        }).implicitMappings();
        log.debug("configured model mapper {}", modelMapper.getTypeMaps());
    }

    private Participant getRawParticipantOrThrow(UUID id) {
        return participantRepository.findById(id).orElseThrow(() ->
                {
                    IllegalArgumentException ex = new IllegalArgumentException("Executor with id '%s' not found".formatted(id));
                    log.debug("getRawParticipantOrThrow()", ex);
                    return ex;
                }
        );
    }

    @Override
    public UUID recordExecutionFact(@NotNull @Valid RecordExecutionFactDto factDto) {
        if (factDto.getFinishTime() != null && factDto.getStartTime() == null) {
            IllegalArgumentException ex = new IllegalArgumentException("Start time must be specified if finish time is");
            log.debug("recordExecutionFact()", ex);
            throw ex;
        }
        log.debug("mapping RecordExecutionFactDto {}", factDto);
        ExecutionFact executionFact = modelMapper.map(factDto, ExecutionFact.class);
        log.debug("saving execution fact {}", executionFact);
        ExecutionFact saved = factRepository.save(executionFact);
        log.debug("saved execution fact {}", saved);
        return saved.getId();
    }

    @Override
    public GetDetailedExecutionFactDto getById(@NotNull UUID id) {
        log.debug("getting execution fact by id {}", id);
        ExecutionFact got = getRawFactOrThrowNotFound(id);
        log.debug("got {}", got);
        GetDetailedExecutionFactDto mapped = modelMapper.map(
                got,
                GetDetailedExecutionFactDto.class
        );
        log.debug("mapped to GetDetailedExecutionFactDto {}", mapped);
        return mapped;
    }

    private ExecutionFact getRawFactOrThrowNotFound(UUID id) {
        return factRepository.findById(id).orElseThrow(() ->
                {
                    IllegalArgumentException ex = new IllegalArgumentException("Execution fact with id '%s' not found".formatted(id));
                    log.debug("getRawFactOrThrowNotFound()", ex);
                    return ex;
                }
        );
    }

    @Override
    public void updateExecutionFact(@NotNull UUID id, @NotNull @Valid UpdateExecutionFactDto factDto) {
        log.debug("updating execution fact with id {} with data {}", id, factDto);
        ExecutionFact fact = getRawFactOrThrowNotFound(id);
        log.debug("found execution fact {}", fact);
        if (factDto.getDescription() != null) {
            fact.setDescription(factDto.getDescription());
        }
        if (factDto.getExecutorId() != null) {
            fact.setExecutor(getRawParticipantOrThrow(factDto.getExecutorId()));
        }
        if (factDto.getStartTime() != null) {
            fact.setStartTime(factDto.getStartTime());
        }
        if (factDto.getFinishTime() != null) {
            fact.setFinishTime(factDto.getFinishTime());
        }
        log.debug("updating execution fact with data {}", fact);
    }

    @Override
    public void deleteById(@NotNull UUID id) {
        log.debug("deleting execution fact with id {}", id);
        factRepository.deleteById(id);
    }

    @Override
    public GetFilteredExecutionFactsDto findAll(@NotNull @Valid ExecutionFactFilterOptionsDto factFilterOptionsDto) {
        log.debug("finding execution facts by filter {}", factFilterOptionsDto);
        validatePageSize(factFilterOptionsDto);
        assignDefaultValues(factFilterOptionsDto);
        Page<ExecutionFact> pageRequestResult = factRepository.findAll(
                executionFactSpecs.byFilterDto(factFilterOptionsDto),
                PageRequest.of(factFilterOptionsDto.getPageIndex(), factFilterOptionsDto.getPageSize())
        );
        List<ExecutionFact> found = pageRequestResult.getContent();
        log.debug("found execution facts {}", found);
        List<GetExecutionFactDto> returned = found.stream()
                .map(entity -> modelMapper.map(entity, GetExecutionFactDto.class))
                .toList();
        log.debug("mapped execution facts {}", returned);
        return GetFilteredExecutionFactsDto.builder()
                .withExecutionFacts(returned)
                .withTotalPages(pageRequestResult.getTotalPages())
                .build();
    }

    private void validatePageSize(ExecutionFactFilterOptionsDto factFilterOptionsDto) {
        Integer pageSize = factFilterOptionsDto.getPageSize();
        if (pageSize != null && pageSize > getFactsMaxPageSize) {
            IllegalArgumentException ex = new IllegalArgumentException("pageSize: must be less then or equal to %s".formatted(getFactsMaxPageSize));
            log.debug("validatePageSize()", ex);
            throw ex;
        }
    }


    private void assignDefaultValues(ExecutionFactFilterOptionsDto factFilterOptionsDto) {
        if (factFilterOptionsDto.getPageIndex() == null) {
            factFilterOptionsDto.setPageIndex(0);
        }
        if (factFilterOptionsDto.getPageSize() == null) {
            factFilterOptionsDto.setPageSize(50);
        }
        log.debug("assigned default data to ExecutionFactFilterOptionsDto {}", factFilterOptionsDto);
    }

    @Override
    public ByteArrayInputStream generateCsvReport(@NotNull @Valid ExecutionFactFilterOptionsDto factFilterOptionsDto) {
        log.debug("generating csv report for execution facts for filter {}", factFilterOptionsDto);
        try (
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                CSVPrinter csvPrinter = new CSVPrinter(new PrintWriter(out), CSVFormat.DEFAULT)
        ) {
            Object[] titles = {"id", "start_time", "finish_time",
                    "executor_full_name", "executor_id", "description"};
            csvPrinter.printRecord(titles);
            log.debug("added titles {}", titles);
            for (GetExecutionFactDto fact : findAll(factFilterOptionsDto).getExecutionFacts()) {
                List<String> data = Arrays.asList(
                        String.valueOf(fact.getId()),
                        fact.getStartTime().format(DateTimeFormatter.ISO_DATE_TIME),
                        fact.getFinishTime().format(DateTimeFormatter.ISO_DATE_TIME),
                        fact.getExecutorFullName(),
                        String.valueOf(fact.getExecutorId()),
                        fact.getDescription()
                );
                csvPrinter.printRecord(data);
                log.debug("added row {}", data);
            }
            csvPrinter.flush();
            log.debug("finished generating csv");
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            UncheckedIOException ex = new UncheckedIOException("Fail to import data to CSV file: " + e.getMessage(), e);
            log.debug("generateCsvReport()", ex);
            throw ex;
        }
    }

    @Override
    public ExecutionFactUploadResultDto uploadFromFile(@NotNull MultipartFile multipart) {
        log.debug("uploading data from file");
        try {
            byte[] fileBytes = multipart.getBytes();
            int importedCount = 0;
            int failedCount = 0;
            List<RecordFactToItsValidationExceptions> invalidRecordDtosToItsValidationExceptions = new LinkedList<>();
            List<RecordExecutionFactDto> factDtos = objectMapper.readValue(fileBytes, new TypeReference<>() {});
            for(RecordExecutionFactDto dto : factDtos) {
                Optional<RecordFactToItsValidationExceptions> exceptions = resolveFact(dto);
                if(exceptions.isPresent()) {
                    invalidRecordDtosToItsValidationExceptions.add(exceptions.get());
                    failedCount++;
                    log.debug("failed to load {}", exceptions.get());
                } else {
                    importedCount++;
                    log.debug("loaded {}", dto);
                }
            }
            return new ExecutionFactUploadResultDto(importedCount, failedCount, invalidRecordDtosToItsValidationExceptions);
        } catch (IOException e) {
            UncheckedIOException ex = new UncheckedIOException(e);
            log.debug("uploadFromFile()", ex);
            throw ex;
        }
    }

    private Optional<RecordFactToItsValidationExceptions> resolveFact(RecordExecutionFactDto factDto) {
        try {
            Set<ConstraintViolation<RecordExecutionFactDto>> validate = validator.validate(factDto);
            if(validate.isEmpty()) {
                recordExecutionFact(factDto);
                return Optional.empty();
            }
            return Optional.of(
                    new RecordFactToItsValidationExceptions(
                            factDto, toExceptionResponse(new ConstraintViolationException(validate))
                    )
            );
        } catch (MappingException e) {
            return Optional.of(
                    new RecordFactToItsValidationExceptions(factDto, toExceptionResponse(e.getCause()))
            );
        }
    }

    private ExceptionResponse toExceptionResponse(Throwable e) {
        return ExceptionResponse.builder()
                        .withHttpStatus(HttpStatus.BAD_REQUEST)
                        .withDate(dateTimeService.utcNow())
                        .withMessage(e.getMessage())
                        .build();
    }
}
