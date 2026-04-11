package task_tracker.tasks.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import task_tracker.tasks.entity.Status;

import java.time.LocalDate;

public record TaskCreateDTO(
        @NotNull(message = "Поле task name обязательно!")
        @Size(min = 1, max = 50, message = "Поле task name не должно быть пустым или больше 50 символов!")
        String taskName,

        @Schema(
                description = "Текущий статус задачи",
                example = "IN_PROGRESS",
                defaultValue = "TO_DO"
        )
        Status status,

        @NotNull(message = "Поле description обязательно!")
        @Size(min = 1, message = "Поле description не должно быть пустым!")
        String description,

        @NotNull(message = "поле completedAt должно быть обязательно!")
        LocalDate completedAt
) {
}
