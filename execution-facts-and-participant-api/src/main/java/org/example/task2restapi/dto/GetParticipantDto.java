package org.example.task2restapi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetParticipantDto {

    private UUID id;

    private String fullName;

    private String email;

}
