package task_tracker_mail.dto;

import java.util.List;

public record MixedReminderDTO(
        String email,
        List<TaskDTO> uncompleted,
        long uncompletedCount,
        List<TaskDTO> completed,
        long completedCount) {
}
