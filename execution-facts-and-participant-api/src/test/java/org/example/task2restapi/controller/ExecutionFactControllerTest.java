package org.example.task2restapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.example.task2restapi.Task2RestApiApplication;
import org.example.task2restapi.config.TestDbConfig;
import org.example.task2restapi.dto.ExecutionFactFilterOptionsDto;
import org.example.task2restapi.dto.ExecutionFactUploadResultDto;
import org.example.task2restapi.dto.GetDetailedExecutionFactDto;
import org.example.task2restapi.dto.GetExecutionFactDto;
import org.example.task2restapi.dto.GetFilteredExecutionFactsDto;
import org.example.task2restapi.dto.GetParticipantDto;
import org.example.task2restapi.dto.RecordExecutionFactDto;
import org.example.task2restapi.dto.RecordFactToItsValidationExceptions;
import org.example.task2restapi.dto.UpdateExecutionFactDto;
import org.example.task2restapi.entity.ExecutionFact;
import org.example.task2restapi.entity.Participant;
import org.example.task2restapi.repository.ExecutionFactRepository;
import org.example.task2restapi.repository.ParticipantRepository;
import org.example.task2restapi.service.DateTimeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = {Task2RestApiApplication.class, TestDbConfig.class}
)
@TestPropertySource(properties = {
        """
        spring.liquibase.contexts=none
        spring.cloud.consul.enabled=false
        """
})
@AutoConfigureMockMvc
class ExecutionFactControllerTest {

    @Autowired
    MockMvc mockMvc;

    @SpyBean
    ParticipantRepository participantRepository;

    @SpyBean
    ExecutionFactRepository executionFactRepository;

    @Autowired
    ObjectMapper objectMapper;

    @SpyBean
    DateTimeService timeService;

    @Autowired
    @Qualifier("executionFactsMaxPageSize")
    Integer executionFactsMaxPageSize;

    List<Participant> initialParticipants = List.of(
            Participant.builder()
                    .withFullName("Test 1")
                    .withEmail("test1@gmail.com")
                    .build(),
            Participant.builder()
                    .withFullName("Test 2")
                    .withEmail("test2@gmail.com")
                    .build()
    );

    @BeforeEach
    void initDb() {
        initialParticipants = participantRepository.saveAllAndFlush(initialParticipants);
        reset(participantRepository, executionFactRepository);
    }

    @AfterEach
    void clearDb() {
        executionFactRepository.deleteAll();
        participantRepository.deleteAll();
    }

    ExecutionFact saveAndGetStandardExecutionFactFromDb() {
        return saveAndGetExecutionFactFromDb(1, initialParticipants.get(0), "a");
    }

    ExecutionFact saveAndGetExecutionFactFromDb(int startMonth, Participant executor, String descriptionCharacter) {
        LocalDateTime startTime = LocalDateTime.of(2000, startMonth, 1, 0, 0);
        LocalDateTime finishTime = startTime.plusMinutes(10);
        String description = String.join("", Collections.nCopies(500, descriptionCharacter));
        ExecutionFact executionFact = ExecutionFact.builder()
                .withExecutor(executor)
                .withDescription(description)
                .withFinishTime(finishTime)
                .withStartTime(startTime)
                .build();
        return executionFactRepository.saveAndFlush(executionFact);
    }

    @SneakyThrows
    @Test
    void recordExecutionFact_shouldSaveGivenDataAndReturnItsID_ifItIsValid() {
        //given
        LocalDateTime startTime = LocalDateTime.of(2000, 1, 1, 0, 0);
        LocalDateTime finishTime = startTime.plusMinutes(10);
        UUID participantId = initialParticipants.get(0).getId();
        String description = String.join("", Collections.nCopies(500, "a"));
        RecordExecutionFactDto executionFactDto = new RecordExecutionFactDto(
                participantId, description, startTime, finishTime
        );
        //when
        String result = mockMvc.perform(
                        post("/api/v1/execution-facts")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(executionFactDto)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        //then
        UUID id = assertDoesNotThrow(
                () -> UUID.fromString(objectMapper.readValue(result, Map.class).get("id").toString()),
                "Returned id should be valid."
        );
        ExecutionFact fromDb = executionFactRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Given fact was not saved."));
        assertEquals(executionFactDto.getExecutorId(), fromDb.getExecutor().getId());
        assertEquals(executionFactDto.getDescription(), fromDb.getDescription());
        assertEquals(executionFactDto.getStartTime(), fromDb.getStartTime());
        assertEquals(executionFactDto.getFinishTime(), fromDb.getFinishTime());
    }

    @SneakyThrows
    @Test
    void recordExecutionFact_shouldSaveWithDefaultStartTime_ifNoneWasPresentInRecordDTO() {
        //given
        LocalDateTime startTime = LocalDateTime.of(2010, 1, 1, 0, 0);
        UUID participantId = initialParticipants.get(0).getId();
        String description = String.join("", Collections.nCopies(500, "a"));
        RecordExecutionFactDto executionFactDto = new RecordExecutionFactDto(
                participantId, description, null, null
        );
        doReturn(startTime).when(timeService).utcNow();
        //when
        String result = mockMvc.perform(
                        post("/api/v1/execution-facts")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(executionFactDto)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        //then
        UUID id = assertDoesNotThrow(
                () -> UUID.fromString(objectMapper.readValue(result, Map.class).get("id").toString()),
                "Returned id should be valid."
        );
        ExecutionFact fromDb = executionFactRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Given fact was not saved."));
        assertEquals(executionFactDto.getExecutorId(), fromDb.getExecutor().getId());
        assertEquals(executionFactDto.getDescription(), fromDb.getDescription());
        assertEquals(startTime, fromDb.getStartTime());
        assertEquals(executionFactDto.getFinishTime(), fromDb.getFinishTime());
    }

    @SneakyThrows
    @Test
    void recordExecutionFact_shouldReturnExceptionResponse_ifRecordDtoContainsFinishTimeButDoesNotStartTime() {
        //given
        LocalDateTime startTime = null;
        LocalDateTime finishTime = LocalDateTime.of(2000, 1, 1, 0, 0);
        UUID participantId = initialParticipants.get(0).getId();
        String description = String.join("", Collections.nCopies(500, "a"));
        RecordExecutionFactDto executionFactDto = new RecordExecutionFactDto(
                participantId, description, startTime, finishTime
        );
        //when
        String result = mockMvc.perform(
                        post("/api/v1/execution-facts")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(executionFactDto)))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();
        //then
        ExceptionResponse exception = assertDoesNotThrow(
                () -> objectMapper.readValue(result, ExceptionResponse.class),
                "It should return exception"
        );
        assertTrue(
                exception.getMessage().contains("Start time must be specified if finish time is"),
                "Should contain message"
        );
    }

    @SneakyThrows
    @Test
    void recordExecutionFact_shouldReturnResponseContainingAllValidationErrorsAndShouldNotSaveInvalidData() {
        //given
        LocalDateTime startTime = LocalDateTime.of(2000, 1, 1, 0, 0);
        LocalDateTime finishTime = startTime.minusMinutes(10);
        UUID participantId = initialParticipants.get(0).getId();
        String description = String.join("", Collections.nCopies(501, "a"));
        RecordExecutionFactDto executionFactDto = new RecordExecutionFactDto(
                participantId, description, startTime, finishTime
        );
        //when
        String result = mockMvc.perform(
                        post("/api/v1/execution-facts")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(executionFactDto)))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();
        //then
        ExceptionResponse exceptions = assertDoesNotThrow(() -> objectMapper.readValue(result, ExceptionResponse.class), "It should return exceptions");
        assertTrue(exceptions.getMessage().contains("factDto: startTime is after finishTime"),
                "Should contain date time range validation message.");
        assertTrue(exceptions.getMessage().contains("factDto.description: length must be between 1 and 500"),
                "Should contain description length validation message.");
        assertTrue(executionFactRepository.findAll().isEmpty(), "Execution facts table should be empty");
    }

    @Test
    @SneakyThrows
    void recordExecutionFact_shouldReturnExceptionResponse_ifGivenExecutorIdDoesNotExist() {
        //given
        LocalDateTime startTime = LocalDateTime.of(2000, 1, 1, 0, 0);
        LocalDateTime finishTime = startTime.plusMinutes(10);
        UUID participantId = initialParticipants.get(0).getId();
        String description = String.join("", Collections.nCopies(500, "a"));
        RecordExecutionFactDto executionFactDto = new RecordExecutionFactDto(
                participantId, description, startTime, finishTime
        );
        participantRepository.deleteAll();
        participantRepository.flush();
        //when
        String result = mockMvc.perform(
                        post("/api/v1/execution-facts")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(executionFactDto)))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();
        //then
        ExceptionResponse exceptions = assertDoesNotThrow(() -> objectMapper.readValue(result, ExceptionResponse.class), "It should return exceptions");
        assertTrue(exceptions.getMessage().contains("Executor with id '%s' not found".formatted(participantId)),
                "Should contain executor not found message.");
        assertTrue(executionFactRepository.findAll().isEmpty(), "Execution facts table should be empty");
    }

    @SneakyThrows
    @Test
    void getById_shouldReturnDetailedExecutionFactDto_ifThereIsAnyWithGivenId() {
        //given
        ExecutionFact expectedExecutionFact = saveAndGetStandardExecutionFactFromDb();
        //when
        String result = mockMvc.perform(
                        get("/api/v1/execution-facts/" + expectedExecutionFact.getId()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        //then
        GetDetailedExecutionFactDto dto = assertDoesNotThrow(
                () -> objectMapper.readValue(result, GetDetailedExecutionFactDto.class),
                "It should return valid get dto"
        );
        assertEquals(expectedExecutionFact.getDescription(), dto.getDescription());
        assertEquals(expectedExecutionFact.getStartTime(), dto.getStartTime());
        assertEquals(expectedExecutionFact.getFinishTime(), dto.getFinishTime());
        assertEquals(expectedExecutionFact.getId(), dto.getId());
        Participant expectedExecutor = expectedExecutionFact.getExecutor();
        GetParticipantDto getParticipantDto = dto.getExecutor();
        assertEquals(expectedExecutor.getEmail(), getParticipantDto.getEmail());
        assertEquals(expectedExecutor.getFullName(), getParticipantDto.getFullName());
    }

    @Test
    @SneakyThrows
    void getById_shouldReturnExceptionResponse_ifExecutorFactWithGivenIdDoesNotExist() {
        //given
        UUID factId = UUID.randomUUID();
        //when
        String result = mockMvc.perform(
                        get("/api/v1/execution-facts/" + factId))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();
        //then
        ExceptionResponse exceptions = assertDoesNotThrow(() -> objectMapper.readValue(result, ExceptionResponse.class), "It should return exceptions");
        assertTrue(exceptions.getMessage().contains("Execution fact with id '%s' not found".formatted(factId)),
                "Should contain execution fact not found message.");
    }

    @Test
    @SneakyThrows
    void updateExecutionFact_shouldUpdateExecutionFactWithAllNewData() {
        //given
        ExecutionFact saved = saveAndGetStandardExecutionFactFromDb();
        LocalDateTime newStartTime = LocalDateTime.of(2000, 2, 1, 0, 0);
        LocalDateTime newFinishTime = newStartTime.plusMinutes(10);
        UUID newExecutorId = initialParticipants.get(1).getId();
        String newDescription = String.join("", Collections.nCopies(500, "b"));
        UpdateExecutionFactDto updateDto = new UpdateExecutionFactDto(
                newStartTime, newFinishTime, newDescription, newExecutorId
        );
        //when
        mockMvc.perform(
                        put("/api/v1/execution-facts/" + saved.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk());
        //then
        ExecutionFact updated = executionFactRepository.findById(saved.getId()).get();
        assertEquals(updateDto.getStartTime(), updated.getStartTime());
        assertEquals(updateDto.getFinishTime(), updated.getFinishTime());
        assertEquals(updateDto.getDescription(), updated.getDescription());
        assertEquals(updateDto.getExecutorId(), updated.getExecutor().getId());
    }

    @Test
    @SneakyThrows
    void updateExecutionFact_shouldModifyOnlyGivenData() {
        //given
        ExecutionFact saved = saveAndGetStandardExecutionFactFromDb();
        UUID newExecutorId = initialParticipants.get(1).getId();
        String newDescription = String.join("", Collections.nCopies(500, "b"));
        UpdateExecutionFactDto updateDto = new UpdateExecutionFactDto(
                null, null, newDescription, newExecutorId
        );
        //when
        mockMvc.perform(
                        put("/api/v1/execution-facts/" + saved.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk());
        //then
        ExecutionFact updated = executionFactRepository.findById(saved.getId()).get();
        assertEquals(saved.getStartTime(), updated.getStartTime());
        assertEquals(saved.getFinishTime(), updated.getFinishTime());
        assertEquals(updateDto.getDescription(), updated.getDescription());
        assertEquals(updateDto.getExecutorId(), updated.getExecutor().getId());
    }

    @Test
    @SneakyThrows
    void updateExecutionFact_shouldReturnExceptionResponse_ifGivenUpdateDtoIsNotValid() {
        //given
        ExecutionFact saved = saveAndGetStandardExecutionFactFromDb();
        LocalDateTime newStartTime = LocalDateTime.of(2000, 2, 1, 0, 0);
        LocalDateTime newFinishTime = newStartTime.minusMinutes(10);
        UUID newExecutorId = initialParticipants.get(1).getId();
        String newDescription = String.join("", Collections.nCopies(501, "b"));
        UpdateExecutionFactDto updateDto = new UpdateExecutionFactDto(
                newStartTime, newFinishTime, newDescription, newExecutorId
        );
        //when
        String result = mockMvc.perform(
                        put("/api/v1/execution-facts/" + saved.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();
        //then
        ExceptionResponse exceptions = assertDoesNotThrow(() -> objectMapper.readValue(result, ExceptionResponse.class), "It should return exceptions");
        assertTrue(exceptions.getMessage().contains("updateExecutionFact.factDto: startTime is after finishTime"),
                "Should contain date time range validation message.");
        assertTrue(exceptions.getMessage().contains("updateExecutionFact.factDto.description: length must be between 1 and 500"),
                "Should contain description length validation message.");
        ExecutionFact updated = executionFactRepository.findById(saved.getId()).get();
        assertEquals(saved.getStartTime(), updated.getStartTime());
        assertEquals(saved.getFinishTime(), updated.getFinishTime());
        assertEquals(saved.getDescription(), updated.getDescription());
        assertEquals(saved.getExecutor().getId(), updated.getExecutor().getId());
    }

    @Test
    @SneakyThrows
    void updateExecutionFact_shouldReturnExceptionResponse_ifGivenNewExecutorIdWasNotfound() {
        //given
        ExecutionFact saved = saveAndGetStandardExecutionFactFromDb();
        UUID newExecutorId = UUID.randomUUID();
        UpdateExecutionFactDto updateDto = new UpdateExecutionFactDto(
                null, null, null, newExecutorId
        );
        //when
        String result = mockMvc.perform(
                        put("/api/v1/execution-facts/" + saved.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();
        //then
        ExceptionResponse exceptions = assertDoesNotThrow(() -> objectMapper.readValue(result, ExceptionResponse.class), "It should return exceptions");
        assertTrue(exceptions.getMessage().contains("Executor with id '%s' not found".formatted(newExecutorId)),
                "Should contain executor not found message.");
        ExecutionFact updated = executionFactRepository.findById(saved.getId()).get();
        assertEquals(saved.getStartTime(), updated.getStartTime());
        assertEquals(saved.getFinishTime(), updated.getFinishTime());
        assertEquals(saved.getDescription(), updated.getDescription());
        assertEquals(saved.getExecutor().getId(), updated.getExecutor().getId());
    }

    @Test
    @SneakyThrows
    void updateExecutionFact_shouldReturnExceptionResponse_ifGivenExecutionFactIdWasNotfound() {
        //given
        ExecutionFact saved = saveAndGetStandardExecutionFactFromDb();
        LocalDateTime newStartTime = LocalDateTime.of(2000, 2, 1, 0, 0);
        LocalDateTime newFinishTime = newStartTime.plusMinutes(10);
        UUID newExecutorId = initialParticipants.get(1).getId();
        String newDescription = String.join("", Collections.nCopies(500, "b"));
        UpdateExecutionFactDto updateDto = new UpdateExecutionFactDto(
                newStartTime, newFinishTime, newDescription, newExecutorId
        );
        UUID factId = UUID.randomUUID();
        //when
        String result = mockMvc.perform(
                        put("/api/v1/execution-facts/" + factId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();
        //then
        ExceptionResponse exceptions = assertDoesNotThrow(() -> objectMapper.readValue(result, ExceptionResponse.class), "It should return exceptions");
        assertTrue(exceptions.getMessage().contains("Execution fact with id '%s' not found".formatted(factId)),
                "Should contain execution fact not found message.");
        ExecutionFact updated = executionFactRepository.findById(saved.getId()).get();
        assertEquals(saved.getStartTime(), updated.getStartTime());
        assertEquals(saved.getFinishTime(), updated.getFinishTime());
        assertEquals(saved.getDescription(), updated.getDescription());
        assertEquals(saved.getExecutor().getId(), updated.getExecutor().getId());
    }

    @Test
    @SneakyThrows
    void deleteExecutionFact_shouldDeleteExecutionFactWithGivenId() {
        //given
        ExecutionFact saved = saveAndGetStandardExecutionFactFromDb();
        ExecutionFact expectedToBePresent = saveAndGetExecutionFactFromDb(3, initialParticipants.get(0), "n");
        //when
        mockMvc.perform(
                        delete("/api/v1/execution-facts/" + saved.getId()))
                .andExpect(status().isOk());
        //then
        assertTrue(executionFactRepository.findById(saved.getId()).isEmpty(), "Db should not contain deleted fact");
        assertTrue(executionFactRepository.findById(expectedToBePresent.getId()).isPresent(), "It should not affect other records in db");
    }

    @Test
    @SneakyThrows
    void deleteExecutionFact_shouldIgnore_ifGivenFactIdDoesNotExist() {
        //given
        ExecutionFact saved = saveAndGetStandardExecutionFactFromDb();
        UUID factId = UUID.randomUUID();
        //when
        mockMvc.perform(delete("/api/v1/execution-facts/" + factId))
                .andExpect(status().isOk());
        //then
        assertTrue(executionFactRepository.findById(saved.getId()).isPresent(), "It should not affect other records in db");
    }

    @Test
    @SneakyThrows
    void getFiltered_shouldReturnRightDataForGivenFilter() {
        //given
        saveRandomFacts();
        Participant expectedParticipant = initialParticipants.get(0);
        String executorEmail = expectedParticipant.getEmail();
        LocalDateTime fromFinishTime = LocalDateTime.of(2003, 1, 1, 0, 0);
        LocalDateTime toFinishTime = fromFinishTime.plusYears(1);
        String description = "expected description";
        Integer pageIndex = 1;
        Integer pageSize = 1;
        ExecutionFactFilterOptionsDto filterOptionsDto = new ExecutionFactFilterOptionsDto(
                executorEmail, fromFinishTime, toFinishTime, description, pageIndex, pageSize
        );
        LocalDateTime finishTime = findMiddleDateTime(fromFinishTime, toFinishTime);
        ExecutionFact firstSuitable = executionFactRepository.saveAndFlush(ExecutionFact.builder()
                .withDescription(description)
                .withExecutor(expectedParticipant)
                .withStartTime(finishTime.minusMonths(4))
                .withFinishTime(finishTime)
                .build());
        ExecutionFact secondSuitable = executionFactRepository.saveAndFlush(ExecutionFact.builder()
                .withDescription(description)
                .withExecutor(expectedParticipant)
                .withStartTime(finishTime.minusMonths(2))
                .withFinishTime(finishTime)
                .build());
        //when
        String resultJson = mockMvc.perform(
                        post("/api/v1/execution-facts/_list")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(filterOptionsDto)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn().getResponse().getContentAsString();
        //then
        GetFilteredExecutionFactsDto actualFacts = objectMapper.readValue(resultJson, GetFilteredExecutionFactsDto.class);
        List<GetExecutionFactDto> facts = actualFacts.getExecutionFacts();
        assertEquals(1, facts.size(),
                "It should contain only one object as filter has pageSize specified");
        assertTrue(getDtoEqualsEntity(firstSuitable, facts.get(0)) || getDtoEqualsEntity(secondSuitable, facts.get(0)),
                "It should contain one of the suitable facts fot given filter");
        assertEquals(2, actualFacts.getTotalPages(), "should have right amount of total pages");
    }

    private void saveRandomFacts() {
        String[] characters = new String[]{
                "a", "b", "c", "d", "e", "f", "g"
        };
        List<ExecutionFact> allFacts = IntStream.range(0, 10).mapToObj(
                num -> saveAndGetExecutionFactFromDb(
                        num + 1,
                        initialParticipants.get(num % initialParticipants.size()),
                        characters[num % characters.length]
                )
        ).toList();
        executionFactRepository.saveAllAndFlush(allFacts);
    }

    private LocalDateTime findMiddleDateTime(LocalDateTime dateTime1, LocalDateTime dateTime2) {
        ZoneOffset systemDefaultOffset = OffsetDateTime.now().getOffset();
        long millis1 = dateTime1.toInstant(systemDefaultOffset).toEpochMilli();
        long millis2 = dateTime2.toInstant(systemDefaultOffset).toEpochMilli();
        long middleMillis = (millis1 + millis2) / 2;
        return LocalDateTime.ofEpochSecond(middleMillis / 1000, 0, systemDefaultOffset);
    }

    private boolean getDtoEqualsEntity(ExecutionFact entity, GetExecutionFactDto factDto) {
        return entity.getId().equals(factDto.getId()) &&
               entity.getExecutor().getId().equals(factDto.getExecutorId()) &&
               entity.getExecutor().getFullName().equals(factDto.getExecutorFullName()) &&
               entity.getDescription().equals(factDto.getDescription()) &&
               entity.getStartTime().equals(factDto.getStartTime()) &&
               entity.getFinishTime().equals(factDto.getFinishTime());
    }

    @Test
    @SneakyThrows
    void getFiltered_shouldReturnExceptionMessageContainingAllValidationErrors_ifGivenInvalidDto() {
        //given
        String executorEmail = "bad%email";
        LocalDateTime fromFinishTime = LocalDateTime.of(2003, 1, 1, 0, 0);
        LocalDateTime toFinishTime = fromFinishTime.minusMonths(1);
        String description = String.join("", Collections.nCopies(501, "a"));
        Integer pageIndex = -1;
        Integer pageSize = 0;
        ExecutionFactFilterOptionsDto filterOptionsDto = new ExecutionFactFilterOptionsDto(
                executorEmail, fromFinishTime, toFinishTime, description, pageIndex, pageSize
        );
        //when
        String resultJson = mockMvc.perform(
                        post("/api/v1/execution-facts/_list")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(filterOptionsDto)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn().getResponse().getContentAsString();
        //then
        ExceptionResponse exceptions = assertDoesNotThrow(() -> objectMapper.readValue(resultJson, ExceptionResponse.class), "It should return exceptions");
        assertTrue(exceptions.getMessage().contains(
                        "pageSize: must be greater than 0"),
                "no pageSize message"
        );
        assertTrue(exceptions.getMessage().contains(
                        "factFilterOptionsDto: fromFinishTime is after toFinishTime"),
                "no range validation message"
        );
        assertTrue(exceptions.getMessage().contains(
                        "description: length must be between 1 and 500"),
                "no description message"
        );
        assertTrue(exceptions.getMessage().contains(
                        "pageIndex: must be greater than or equal to 0"),
                "no pageIndex message"
        );
        assertTrue(exceptions.getMessage().contains(
                        "executorEmail: must be a well-formed email address"),
                "no executorEmail message"
        );
    }

    @Test
    @SneakyThrows
    void getFiltered_shouldReturnExceptionMessage_ifGivenPageSizeExceedsTheLimit() {
        //given
        Participant expectedParticipant = initialParticipants.get(0);
        String executorEmail = expectedParticipant.getEmail();
        LocalDateTime fromFinishTime = LocalDateTime.of(2003, 1, 1, 0, 0);
        LocalDateTime toFinishTime = fromFinishTime.plusYears(1);
        String description = "expected description";
        Integer pageIndex = 1;
        Integer pageSize = executionFactsMaxPageSize + 1;
        ExecutionFactFilterOptionsDto filterOptionsDto = new ExecutionFactFilterOptionsDto(
                executorEmail, fromFinishTime, toFinishTime, description, pageIndex, pageSize
        );
        //when
        String resultJson = mockMvc.perform(
                        post("/api/v1/execution-facts/_list")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(filterOptionsDto)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn().getResponse().getContentAsString();
        //then
        ExceptionResponse exceptions = assertDoesNotThrow(() -> objectMapper.readValue(resultJson, ExceptionResponse.class), "It should return exceptions");
        assertTrue(exceptions.getMessage().contains(
                        "pageSize: must be less then or equal to %s".formatted(executionFactsMaxPageSize)),
                "no pageSize max value message"
        );
    }

    @Test
    @SneakyThrows
    void generateReport_shouldGenerateRightReportForGivenFilter() {
        //given
        saveRandomFacts();
        Participant expectedParticipant = initialParticipants.get(0);
        String executorEmail = expectedParticipant.getEmail();
        LocalDateTime fromFinishTime = LocalDateTime.of(2003, 1, 1, 0, 0);
        LocalDateTime toFinishTime = fromFinishTime.plusYears(1);
        String description = "expected description";
        Integer pageIndex = 1;
        Integer pageSize = 1;
        ExecutionFactFilterOptionsDto filterOptionsDto = new ExecutionFactFilterOptionsDto(
                executorEmail, fromFinishTime, toFinishTime, description, pageIndex, pageSize
        );
        LocalDateTime finishTime = findMiddleDateTime(fromFinishTime, toFinishTime);
        ExecutionFact firstSuitable = executionFactRepository.saveAndFlush(ExecutionFact.builder()
                .withDescription(description)
                .withExecutor(expectedParticipant)
                .withStartTime(finishTime.minusMonths(4))
                .withFinishTime(finishTime)
                .build());
        ExecutionFact secondSuitable = executionFactRepository.saveAndFlush(ExecutionFact.builder()
                .withDescription(description)
                .withExecutor(expectedParticipant)
                .withStartTime(finishTime.minusMonths(2))
                .withFinishTime(finishTime)
                .build());
        //when
        String resultString = mockMvc.perform(
                        post("/api/v1/execution-facts/_report")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(filterOptionsDto)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.parseMediaType("application/csv")))
                .andReturn().getResponse().getContentAsString();
        //then
        String patter = "id,start_time,finish_time,executor_full_name,executor_id,description\r\n" +
                        "%s,%s,%s,%s,%s,%s\r\n";
        assertTrue(
                patter.formatted(firstSuitable.getId(),
                                firstSuitable.getStartTime().format(DateTimeFormatter.ISO_DATE_TIME),
                                firstSuitable.getFinishTime().format(DateTimeFormatter.ISO_DATE_TIME),
                                firstSuitable.getExecutor().getFullName(), firstSuitable.getExecutor().getId(),
                                firstSuitable.getDescription())
                        .equals(resultString)
                ||
                patter.formatted(secondSuitable.getId(),
                                secondSuitable.getStartTime().format(DateTimeFormatter.ISO_DATE_TIME),
                                secondSuitable.getFinishTime().format(DateTimeFormatter.ISO_DATE_TIME),
                                secondSuitable.getExecutor().getFullName(), secondSuitable.getExecutor().getId(),
                                secondSuitable.getDescription())
                        .equals(resultString),
                "It should contain only one object as filter has pageSize specified");
    }

    @Test
    @SneakyThrows
    void generateReport_shouldReturnExceptionMessageContainingAllValidationErrors_ifGivenInvalidDto() {
        //given
        String executorEmail = "bad%email";
        LocalDateTime fromFinishTime = LocalDateTime.of(2003, 1, 1, 0, 0);
        LocalDateTime toFinishTime = fromFinishTime.minusMonths(1);
        String description = String.join("", Collections.nCopies(501, "a"));
        Integer pageIndex = -1;
        Integer pageSize = 0;
        ExecutionFactFilterOptionsDto filterOptionsDto = new ExecutionFactFilterOptionsDto(
                executorEmail, fromFinishTime, toFinishTime, description, pageIndex, pageSize
        );
        //when
        String resultJson = mockMvc.perform(
                        post("/api/v1/execution-facts/_report")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(filterOptionsDto)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn().getResponse().getContentAsString();
        //then
        ExceptionResponse exceptions = assertDoesNotThrow(() -> objectMapper.readValue(resultJson, ExceptionResponse.class), "It should return exceptions");
        assertTrue(exceptions.getMessage().contains(
                        "pageSize: must be greater than 0"),
                "no pageSize message"
        );
        assertTrue(exceptions.getMessage().contains(
                        "factFilterOptionsDto: fromFinishTime is after toFinishTime"),
                "no range validation message"
        );
        assertTrue(exceptions.getMessage().contains(
                        "description: length must be between 1 and 500"),
                "no description message"
        );
        assertTrue(exceptions.getMessage().contains(
                        "pageIndex: must be greater than or equal to 0"),
                "no pageIndex message"
        );
        assertTrue(exceptions.getMessage().contains(
                        "executorEmail: must be a well-formed email address"),
                "no executorEmail message"
        );
    }

    @Test
    @SneakyThrows
    void generateReport_shouldReturnExceptionMessage_ifGivenPageSizeExceedsTheLimit() {
        //given
        Participant expectedParticipant = initialParticipants.get(0);
        String executorEmail = expectedParticipant.getEmail();
        LocalDateTime fromFinishTime = LocalDateTime.of(2003, 1, 1, 0, 0);
        LocalDateTime toFinishTime = fromFinishTime.plusYears(1);
        String description = "expected description";
        Integer pageIndex = 1;
        Integer pageSize = executionFactsMaxPageSize + 1;
        ExecutionFactFilterOptionsDto filterOptionsDto = new ExecutionFactFilterOptionsDto(
                executorEmail, fromFinishTime, toFinishTime, description, pageIndex, pageSize
        );
        //when
        String resultJson = mockMvc.perform(
                        post("/api/v1/execution-facts/_report")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(filterOptionsDto)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn().getResponse().getContentAsString();
        //then
        ExceptionResponse exceptions = assertDoesNotThrow(() -> objectMapper.readValue(resultJson, ExceptionResponse.class), "It should return exceptions");
        assertTrue(exceptions.getMessage().contains(
                        "pageSize: must be less then or equal to %s".formatted(executionFactsMaxPageSize)),
                "no pageSize max value message"
        );
    }

    @Test
    @SneakyThrows
    void uploadFromFile_shouldSaveValidFactsAndReturnRightResponse() {
        //given
        RecordExecutionFactDto valid1 = RecordExecutionFactDto.builder()
                .withDescription("asdasd")
                .withExecutorId(initialParticipants.get(0).getId())
                .withStartTime(LocalDateTime.of(2003, 1, 1, 0, 0))
                .withFinishTime(LocalDateTime.of(2003, 1, 2, 0, 0))
                .build();
        RecordExecutionFactDto valid2 = RecordExecutionFactDto.builder()
                .withDescription("asdaasdasdad")
                .withExecutorId(initialParticipants.get(1).getId())
                .withStartTime(LocalDateTime.of(2002, 1, 1, 0, 0))
                .withFinishTime(LocalDateTime.of(2004, 1, 2, 0, 0))
                .build();
        RecordExecutionFactDto invalid1 = RecordExecutionFactDto.builder()
                .withStartTime(LocalDateTime.of(2004, 1, 1, 0, 0))
                .withFinishTime(LocalDateTime.of(2003, 1, 2, 0, 0))
                .build();
        RecordExecutionFactDto invalid2 = RecordExecutionFactDto.builder()
                .withDescription(String.join("", Collections.nCopies(501, "b")))
                .build();
        RecordExecutionFactDto invalid3 = RecordExecutionFactDto.builder()
                .withExecutorId(UUID.randomUUID())
                .withDescription("asdasd")
                .build();
        List<RecordExecutionFactDto> facts = List.of(
                valid1,
                invalid1,
                invalid2,
                invalid3,
                valid2
        );
        //when
        String resultJson = mockMvc.perform(
                        multipart(HttpMethod.POST, "/api/v1/execution-facts/upload")
                                .file("file", objectMapper.writeValueAsBytes(facts))
                )
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn().getResponse().getContentAsString();
        //then
        ExecutionFactUploadResultDto resultDto = objectMapper.readValue(resultJson, ExecutionFactUploadResultDto.class);
        assertEquals(3, resultDto.getFailedCount(), "must have right failed count");
        assertEquals(2, resultDto.getImportedCount(), "must have right imported count");
        List<RecordFactToItsValidationExceptions> objectToItsErrors = resultDto.getObjectToItsErrors();
        Optional<RecordFactToItsValidationExceptions> actual1 = objectToItsErrors.stream().filter(o -> invalid1.equals(o.getFactDto())).findFirst();
        assertTrue(
                actual1.isPresent(),
                "must return first invalid object"
        );
        String actual1Messages = actual1.get().getErrors().getMessage();
        assertTrue(actual1Messages.contains("executorId: Execution fact must have executor id"));
        assertTrue(actual1Messages.contains("startTime is after finishTime"));
        assertTrue(actual1Messages.contains("description: Execution fact must have description"));
        Optional<RecordFactToItsValidationExceptions> actual2 = objectToItsErrors.stream().filter(o -> invalid2.equals(o.getFactDto())).findFirst();
        assertTrue(
                actual2.isPresent(),
                "must return second invalid object"
        );
        String actual2Messages = actual2.get().getErrors().getMessage();
        assertTrue(actual2Messages.contains("executorId: Execution fact must have executor id"));
        assertTrue(actual2Messages.contains("description: length must be between 1 and 500"));
        Optional<RecordFactToItsValidationExceptions> actual3 = objectToItsErrors.stream().filter(o -> invalid3.equals(o.getFactDto())).findFirst();
        assertTrue(
                actual3.isPresent(),
                "must return third invalid object"
        );
        String actual3Messages = actual3.get().getErrors().getMessage();
        assertEquals("Executor with id '%s' not found".formatted(invalid3.getExecutorId()), actual3Messages);
    }

}
