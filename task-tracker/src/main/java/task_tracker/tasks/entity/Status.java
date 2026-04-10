package task_tracker.tasks.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        name = "Status",
        description = "Статус задачи",
        enumAsRef = true,
        allowableValues = {"TO_DO", "IN_PROGRESS", "DONE"}
)
public enum Status {
    TO_DO,
    IN_PROGRESS,
    DONE
}
