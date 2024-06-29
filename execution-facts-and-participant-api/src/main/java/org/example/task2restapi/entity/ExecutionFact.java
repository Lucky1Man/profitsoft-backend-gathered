package org.example.task2restapi.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.example.task2restapi.validator.datetime.range.DateTimeRange;
import org.example.task2restapi.validator.datetime.range.DateTimeRangeConstraint;
import org.example.task2restapi.validator.datetime.range.ObjectWithDateTimeRanges;
import org.hibernate.validator.constraints.Length;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity(name = "ExecutionFact")
@Table(
        name = "execution_facts",
        indexes = @Index(name = "description_index", columnList = "description")
)
@Getter
@Setter
@ToString
@Builder(setterPrefix = "with")
@NoArgsConstructor
@AllArgsConstructor
@DateTimeRangeConstraint
public class ExecutionFact implements ObjectWithDateTimeRanges {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, columnDefinition = "uuid")
    private UUID id;

    @Column(
            name = "start_time",
            nullable = false
    )
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @NotNull(message = "Execution fact must have start time")
    private LocalDateTime startTime;

    @Column(name = "finish_time")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime finishTime;

    @NotNull(message = "Execution fact must have executor")
    @ManyToOne(optional = false)
    @JoinColumn(name = "executor_id", referencedColumnName = "id", nullable = false)
    private Participant executor;

    @NotNull(message = "Description must be present.")
    @Column(
            name = "description",
            columnDefinition = "varchar(500)",
            nullable = false
    )
    @Length(min = 1, max = 500)
    private String description;

    @Version
    private Long version;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExecutionFact that = (ExecutionFact) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public List<DateTimeRange> getValidatedRanges() {
        return List.of(
                new DateTimeRange(startTime, finishTime, "startTime", "finishTime")
        );
    }
}
