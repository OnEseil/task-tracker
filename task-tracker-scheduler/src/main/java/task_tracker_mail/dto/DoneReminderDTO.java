package task_tracker_mail.dto;

import java.util.List;

public record DoneReminderDTO(
        String email,
        List<TaskDTO> completed,
        long completedCount) {
}
