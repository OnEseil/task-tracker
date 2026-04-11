package task_tracker.tasks.mapper;

import org.springframework.stereotype.Component;
import task_tracker.auth.entity.User;
import task_tracker.tasks.dto.TaskCreateDTO;
import task_tracker.tasks.dto.TaskDTO;
import task_tracker.tasks.entity.Task;

@Component
public class TaskMapper {

    public TaskDTO toDto(Task task) {
        return new TaskDTO(
                task.getId(),
                task.getTaskName(),
                task.getStatus(),
                task.getDescription(),
                task.getCompletedAt()
        );
    }

    public Task toEntity(TaskCreateDTO taskCreateDTO, User user) {
        return Task.builder()
                .taskName(taskCreateDTO.taskName())
                .status(taskCreateDTO.status())
                .description(taskCreateDTO.description())
                .completedAt(taskCreateDTO.completedAt())
                .user(user)
                .build();
    }
}
