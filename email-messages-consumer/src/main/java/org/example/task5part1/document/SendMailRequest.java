package org.example.task5part1.document;

import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Document(
        indexName = "mails_registry"
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(setterPrefix = "with")
public class SendMailRequest {
    @Id
    private UUID id;
    @Email
    @Field(name = "from")
    private String from;
    @Field(name = "to")
    private List<@Email String> to;
    @Field(name = "subject", type = FieldType.Text)
    private String subject;
    @Field(name = "content", type = FieldType.Text)
    private String content;
    @Field(name = "send_status")
    private SendStatus sendStatus;
    @Field(name = "error_message", type = FieldType.Text)
    private String errorMessage;
    @Field(name = "last_send_attempt", type = FieldType.Date, format = DateFormat.basic_date_time_no_millis)
    private Instant lastSendAttempt;
    @Field(name = "num_send_attempts")
    private Integer numSendAttempts;
    @Field(name = "sent_date", type = FieldType.Date, format = DateFormat.basic_date_time_no_millis)
    private Date sentDate;
}
