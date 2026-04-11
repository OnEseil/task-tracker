package task_tracker.tasks.dto;

import task_tracker.tasks.entity.Status;

import java.time.LocalDate;
import java.util.Optional;

public record TaskPatchUpdateDTO(
        Optional<String> taskName,

        Optional<Status> status,

        Optional<String> description,

        Optional<LocalDate> completedAt
) {
}