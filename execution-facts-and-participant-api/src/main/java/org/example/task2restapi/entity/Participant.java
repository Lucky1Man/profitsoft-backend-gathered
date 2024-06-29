package org.example.task2restapi.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.validator.constraints.Length;

import java.util.Objects;
import java.util.UUID;

@Entity(name = "Participant")
@Table(
        name = "participants",
        uniqueConstraints = @UniqueConstraint(
                name = "participants_email_key",
                columnNames = "email"
        ),
        indexes = {
                @Index(name = "full_name_index", columnList = "full_name")
        }
)
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder(setterPrefix = "with")
public class Participant {
    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, columnDefinition="uuid")
    private UUID id;

    @Column(
            name = "full_name",
            nullable = false,
            columnDefinition = "varchar(100)"
    )
    @Length(min = 1, max = 100)
    @NotNull(message = "Participant must have full name")
    private String fullName;

    @Column(
            name = "email",
            nullable = false,
            columnDefinition = "varchar(320)"
    )
    @Length(min = 3, max = 320)
    @NotNull(message = "Participant must have email")
    private String email;

    @Version
    private Long version;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Participant that = (Participant) o;
        return Objects.equals(id, that.id) && Objects.equals(email, that.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, email);
    }
}
