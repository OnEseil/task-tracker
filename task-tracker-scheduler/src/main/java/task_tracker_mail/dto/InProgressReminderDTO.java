package task_tracker_mail.dto;

import java.util.List;

public record InProgressReminderDTO(
        String email,
        List<TaskDTO> inProgressTasksList,
        long inProgressTasksCount) {
}
