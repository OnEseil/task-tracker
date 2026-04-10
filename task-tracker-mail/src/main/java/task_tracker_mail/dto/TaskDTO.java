package task_tracker_mail.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import task_tracker_mail.model.Status;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ext.javatime.deser.LocalDateDeserializer;
import tools.jackson.databind.ext.javatime.ser.LocalDateSerializer;

import java.time.LocalDate;

public record TaskDTO(
        String taskName,
        Status status,
        String description,
        LocalDate completedAt) {
}
