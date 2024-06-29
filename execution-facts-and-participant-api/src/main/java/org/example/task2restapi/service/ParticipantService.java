package org.example.task2restapi.service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.example.task2restapi.dto.GetParticipantDto;
import org.example.task2restapi.dto.RegisterParticipantDto;
import org.example.task2restapi.dto.UpdateParticipantDto;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.UUID;

/**
 * Provides api for Participants manipulation.
 */
@Validated
public interface ParticipantService {
    /**
     * @return all participant present in db
     */
    List<GetParticipantDto> findAll();

    /**
     * @param participantDto participant to be registered
     * @return id of registered participant
     * @throws IllegalArgumentException given RegisterParticipantDto is invalid
     * @throws jakarta.validation.ConstraintViolationException given RegisterParticipantDto is invalid
     */
    UUID register(@Valid @NotNull RegisterParticipantDto participantDto);

    /**
     * @param id id of participant to be updated
     * @param participantDto data to update participant with
     * @throws IllegalArgumentException given invalid data
     * @throws jakarta.validation.ConstraintViolationException given invalid data
     */
    void updateParticipant(@NotNull UUID id, @Valid @NotNull UpdateParticipantDto participantDto);

    /**
     * Ignores non-existing ids.
     * @param id id of participant to be deleted
     */
    void deleteParticipant(@NotNull UUID id);

    GetParticipantDto getById(@NotNull UUID id);
}
