package org.example.task2restapi.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.example.task2restapi.Task2RestApiApplication;
import org.example.task2restapi.config.TestDbConfig;
import org.example.task2restapi.dto.GetParticipantDto;
import org.example.task2restapi.dto.RegisterParticipantDto;
import org.example.task2restapi.dto.UpdateParticipantDto;
import org.example.task2restapi.entity.Participant;
import org.example.task2restapi.repository.ParticipantRepository;
import org.example.task2restapi.service.DateTimeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
class ParticipantControllerTest {

    @Autowired
    MockMvc mockMvc;

    @SpyBean
    ParticipantRepository participantRepository;

    @Autowired
    ObjectMapper objectMapper;

    @SpyBean
    DateTimeService timeService;

    @BeforeEach
    void initDb() {
        participantRepository.deleteAll();
        participantRepository.flush();
    }

    @Test
    @SneakyThrows
    void getParticipants_shouldReturnAllParticipants() {
        //given
        Participant participant1 = Participant.builder()
                .withEmail("email1@gmail.com")
                .withFullName("name 1")
                .build();
        Participant participant2 = Participant.builder()
                .withEmail("email2@gmail.com")
                .withFullName("name 2")
                .build();
        Participant participant3 = Participant.builder()
                .withEmail("email3@gmail.com")
                .withFullName("name 3")
                .build();
        List<Participant> participants = List.of(
                participant1,
                participant2,
                participant3
        );
        participantRepository.saveAllAndFlush(participants);
        List<GetParticipantDto> expected = List.of(
                new GetParticipantDto(participant1.getId(), participant1.getFullName(), participant1.getEmail()),
                new GetParticipantDto(participant2.getId(), participant2.getFullName(), participant2.getEmail()),
                new GetParticipantDto(participant3.getId(), participant3.getFullName(), participant3.getEmail())
        );
        //when
        String result = mockMvc.perform(get("/api/v1/participants"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        //then
        List<GetParticipantDto> actual = objectMapper.readValue(result, new TypeReference<>() {});
        assertEquals(expected.size(), actual.size(), "size must be same");
        assertTrue(actual.containsAll(expected), "must contain all expected values");
    }

    @Test
    @SneakyThrows
    void register_shouldSaveValidParticipant() {
        //given
        RegisterParticipantDto participantDto = new RegisterParticipantDto("name", "email@gmail.com");
        //when
        String result = mockMvc.perform(
                        post("/api/v1/participants")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(participantDto)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        //then
        UUID id = assertDoesNotThrow(
                () -> UUID.fromString(objectMapper.readValue(result, Map.class).get("id").toString()),
                "Returned id should be valid."
        );
        Participant fromDb = participantRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Given participant was not saved."));
        assertEquals(participantDto.getEmail(), fromDb.getEmail());
        assertEquals(participantDto.getFullName(), fromDb.getFullName());
    }

    @Test
    @SneakyThrows
    void register_shouldReturnExceptionResponse_ifGivenDataIsInvalid() {
        //given
        RegisterParticipantDto participantDto = new RegisterParticipantDto(
                String.join("", Collections.nCopies(101, "a")),
                String.join("", Collections.nCopies(321, "a"))
        );
        //when
        String result = mockMvc.perform(
                        post("/api/v1/participants")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(participantDto)))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();
        //then
        ExceptionResponse exceptions = assertDoesNotThrow(() -> objectMapper.readValue(result, ExceptionResponse.class), "It should return exceptions");
        assertTrue(exceptions.getMessage().contains("email: must be a well-formed email address"),
                "Should contain email format validation message.");
        assertTrue(exceptions.getMessage().contains("email: length must be between 3 and 320"),
                "Should contain email length validation message.");
        assertTrue(exceptions.getMessage().contains("fullName: length must be between 1 and 100"),
                "Should contain full name validation message.");
        assertTrue(participantRepository.findAll().isEmpty(), "Participants table should be empty");
    }

    @Test
    @SneakyThrows
    void register_shouldReturnExceptionResponse_ifGivenEmailIsAlreadyInUse() {
        //given
        String email = "email@gmail.com";
        Participant participant = Participant.builder()
                .withEmail(email)
                .withFullName("name 1")
                .build();
        participantRepository.saveAndFlush(participant);
        RegisterParticipantDto participantDto = new RegisterParticipantDto("name", email);
        //when
        String result = mockMvc.perform(
                        post("/api/v1/participants")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(participantDto)))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();
        //then
        ExceptionResponse exceptions = assertDoesNotThrow(() -> objectMapper.readValue(result, ExceptionResponse.class), "It should return exceptions");
        assertTrue(exceptions.getMessage().contains("Email %s is already taken.".formatted(email)),
                "Should contain email taken message.");
    }

    @Test
    @SneakyThrows
    void updateParticipant_shouldUpdateAllGivenFields() {
        //given
        Participant participant = Participant.builder()
                .withEmail("email@gmail.com")
                .withFullName("full name")
                .build();
        participantRepository.saveAndFlush(participant);
        UpdateParticipantDto updateParticipantDto = new UpdateParticipantDto(
                "new name",
                "newemail@gmail.com"
        );
        //when
        mockMvc.perform(
                        put("/api/v1/participants/{id}", participant.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateParticipantDto)))
                .andExpect(status().isOk());
        //then
        Participant updated = participantRepository.findById(participant.getId()).get();
        assertEquals(updateParticipantDto.getEmail(), updated.getEmail());
        assertEquals(updateParticipantDto.getFullName(), updated.getFullName());
    }

    @Test
    @SneakyThrows
    void updateParticipant_shouldUpdateOnlyGivenFields() {
        //given
        Participant participant = Participant.builder()
                .withEmail("email@gmail.com")
                .withFullName("full name")
                .build();
        participantRepository.saveAndFlush(participant);
        UpdateParticipantDto updateParticipantDto = new UpdateParticipantDto(
                "new name", null
        );
        //when
        mockMvc.perform(
                        put("/api/v1/participants/{id}", participant.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateParticipantDto)))
                .andExpect(status().isOk());
        //then
        Participant updated = participantRepository.findById(participant.getId()).get();
        assertEquals(participant.getEmail(), updated.getEmail());
        assertEquals(updateParticipantDto.getFullName(), updated.getFullName());
    }

    @Test
    @SneakyThrows
    void updateParticipant_shouldReturnExceptionResponse_ifGivenDtoIsInvalid() {
        //given
        Participant participant = Participant.builder()
                .withEmail("email@gmail.com")
                .withFullName("full name")
                .build();
        participantRepository.saveAndFlush(participant);
        UpdateParticipantDto updateParticipantDto = new UpdateParticipantDto(
                String.join("", Collections.nCopies(101, "a")),
                String.join("", Collections.nCopies(321, "a"))
        );
        //when
        String result = mockMvc.perform(
                        put("/api/v1/participants/{id}", participant.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateParticipantDto)))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();
        //then
        ExceptionResponse exceptions = assertDoesNotThrow(() -> objectMapper.readValue(result, ExceptionResponse.class), "It should return exceptions");
        assertTrue(exceptions.getMessage().contains("email: must be a well-formed email address"),
                "Should contain email format validation message.");
        assertTrue(exceptions.getMessage().contains("email: length must be between 3 and 320"),
                "Should contain email length validation message.");
        assertTrue(exceptions.getMessage().contains("fullName: length must be between 1 and 100"),
                "Should contain full name validation message.");
        Participant updated = participantRepository.findById(participant.getId()).get();
        assertEquals(participant.getEmail(), updated.getEmail());
        assertEquals(participant.getFullName(), updated.getFullName());
    }

    @Test
    @SneakyThrows
    void updateParticipant_shouldReturnExceptionResponse_ifGivenIdDoesNotExist() {
        //given
        Participant participant = Participant.builder()
                .withEmail("email@gmail.com")
                .withFullName("full name")
                .build();
        participantRepository.saveAndFlush(participant);
        UpdateParticipantDto updateParticipantDto = new UpdateParticipantDto(
                "new name",
                "newemail@gmail.com"
        );
        UUID id = UUID.randomUUID();
        //when
        String result = mockMvc.perform(
                        put("/api/v1/participants/{id}", id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateParticipantDto)))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();
        //then
        ExceptionResponse exceptions = assertDoesNotThrow(() -> objectMapper.readValue(result, ExceptionResponse.class), "It should return exceptions");
        assertTrue(exceptions.getMessage().contains("Participant with id '%s' not found".formatted(id)),
                "Should contain participant not found message.");
        Participant updated = participantRepository.findById(participant.getId()).get();
        assertEquals(participant.getEmail(), updated.getEmail());
        assertEquals(participant.getFullName(), updated.getFullName());
    }

    @Test
    @SneakyThrows
    void updateParticipant_shouldReturnExceptionResponse_ifGivenEmailIsAlreadyInUse() {
        //given
        String email = "email@gmail.com";
        Participant participant1 = Participant.builder()
                .withEmail(email)
                .withFullName("name 1")
                .build();
        Participant participant2 = Participant.builder()
                .withEmail("email222@gmail.com")
                .withFullName("name 1")
                .build();
        participantRepository.saveAndFlush(participant1);
        participantRepository.saveAndFlush(participant2);
        UpdateParticipantDto updateParticipantDto = new UpdateParticipantDto(
                "name", email
        );
        //when
        String result = mockMvc.perform(
                        put("/api/v1/participants/{id}", participant2.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateParticipantDto)))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();
        //then
        ExceptionResponse exceptions = assertDoesNotThrow(() -> objectMapper.readValue(result, ExceptionResponse.class), "It should return exceptions");
        assertTrue(exceptions.getMessage().contains("Email %s is already taken.".formatted(email)),
                "Should contain email taken message.");
    }

    @Test
    @SneakyThrows
    void deleteParticipant_shouldDeleteParticipantWithGivenId() {
        //given
        Participant participant1 = Participant.builder()
                .withEmail("email1@gmail.com")
                .withFullName("name 1")
                .build();
        Participant participant2 = Participant.builder()
                .withEmail("email2@gmail.com")
                .withFullName("name 2")
                .build();
        List<Participant> participants = List.of(
                participant1,
                participant2
        );
        participantRepository.saveAllAndFlush(participants);
        //when
        mockMvc.perform(
                        delete("/api/v1/participants/{id}", participant1.getId()))
                .andExpect(status().isOk());
        //then
        assertTrue(participantRepository.findById(participant1.getId()).isEmpty(), "Db should not contain deleted participant");
        assertTrue(participantRepository.findById(participant2.getId()).isPresent(), "It should not affect other records in db");
    }

    @Test
    @SneakyThrows
    void deleteParticipant_shouldIgnore_ifGivenParticipantIdDoesNotExist() {
        //given
        Participant participant1 = Participant.builder()
                .withEmail("email1@gmail.com")
                .withFullName("name 1")
                .build();
        List<Participant> participants = List.of(
                participant1
        );
        participantRepository.saveAllAndFlush(participants);
        UUID factId = UUID.randomUUID();
        //when
        mockMvc.perform(delete("/api/v1/participants/{id}", factId))
                .andExpect(status().isOk());
        //then
        assertTrue(participantRepository.findById(participant1.getId()).isPresent(), "It should not affect other records in db");
    }

}
