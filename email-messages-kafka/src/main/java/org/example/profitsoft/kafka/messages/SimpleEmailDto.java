package org.example.profitsoft.kafka.messages;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SimpleEmailDto {
    private String from;
    private List<String> to;
    private String subject;
    private String text;
    private Date sentDate;
}
