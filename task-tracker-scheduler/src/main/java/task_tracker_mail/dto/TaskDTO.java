package task_tracker_mail.dto;

import task_tracker_mail.entity.Status;

import java.time.LocalDate;

public record TaskDTO(
        String taskName,
        Status status,
        String description,
        LocalDate completedAt) {
}
