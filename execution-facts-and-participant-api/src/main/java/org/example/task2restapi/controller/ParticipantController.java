package org.example.task2restapi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.task2restapi.dto.GetParticipantDto;
import org.example.task2restapi.dto.RegisterParticipantDto;
import org.example.task2restapi.dto.UpdateParticipantDto;
import org.example.task2restapi.service.ParticipantService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/participants")
public class ParticipantController {

    private final ParticipantService participantService;

    @GetMapping
    @Operation(
            description = "Returns list of registered participants"
    )
    @ApiResponse(
            responseCode = "200",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    array = @ArraySchema(
                            schema = @Schema(implementation = GetParticipantDto.class)
                    )
            )
    )
    public List<GetParticipantDto> getParticipants() {
        log.debug("getting all participants");
        List<GetParticipantDto> all = participantService.findAll();
        log.debug("got: {}", all);
        return all;
    }

    @PostMapping
    @Operation(
            description = "Register participant."
    )
    @ApiResponse(
            responseCode = "201",
            description = "Participant was successfully registered. It returns id of created participant",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(pattern = """
                            {
                                "id": "0cecc52a-2342-4dfd-83e6-dd6a38a3c119"
                            }
                            """)
            )
    )
    public ResponseEntity<Map<String, UUID>> register(@RequestBody RegisterParticipantDto participantDto) {
        log.debug("registering participant: {}", participantDto);
        UUID id = participantService.register(participantDto);
        log.debug("registered with id: {}", id);
        return ResponseEntity.status(HttpStatus.CREATED.value())
                .body(Map.of("id", id));
    }

    @PutMapping("/{id}")
    @Operation(
            description = "Updates participant with given id with data from UpdateParticipantDto." +
                    " If UpdateParticipantDto has null fields then that specific field will be ignored"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Means that execution fact was updated and all given parameters were changed"
    )
    public void updateParticipant(@PathVariable UUID id, @RequestBody UpdateParticipantDto participantDto) {
        log.debug("updating participant with id {}, with data {}", id, participantDto);
        participantService.updateParticipant(id, participantDto);
        log.debug("updated participant with id {}", id);
    }

    @DeleteMapping("/{id}")
    @Operation(description = "Deletes participant by specified id. If given id does not exist then it is ignored.")
    @ApiResponse(
            responseCode = "200",
            description = "Participant deleted"
    )
    public void deleteParticipant(@PathVariable UUID id) {
        log.debug("deleting participant with id {}", id);
        participantService.deleteParticipant(id);
        log.debug("deleted participant with id {}", id);
    }

    @GetMapping("/{id}")
    @Operation(description = "Returns participant by given id.")
    @ApiResponse(
            responseCode = "200",
            description = "Participant returned.",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = GetParticipantDto.class)
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "Means that participant with given id was not found",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ExceptionResponse.class)
            )
    )
    public GetParticipantDto getById(@PathVariable UUID id) {
        return participantService.getById(id);
    }

}
