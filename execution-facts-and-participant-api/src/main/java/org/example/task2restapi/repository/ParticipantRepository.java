package org.example.task2restapi.repository;

import org.example.task2restapi.entity.Participant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ParticipantRepository extends JpaRepository<Participant, UUID> {

    @Query("select p from Participant p where p.email = :email")
    Optional<Participant> findByEmail(@Param("email") String email);

}
