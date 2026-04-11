package task_tracker.tasks.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import task_tracker.auth.entity.User;
import task_tracker.auth.service.UserService;
import task_tracker.tasks.dto.TaskCreateDTO;
import task_tracker.tasks.dto.TaskDTO;
import task_tracker.tasks.dto.TaskPatchUpdateDTO;
import task_tracker.tasks.entity.Task;
import task_tracker.tasks.mapper.TaskMapper;
import task_tracker.tasks.repository.TaskRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserService userService;
    private final TaskMapper taskMapper;

    @Transactional(readOnly = true)
    public List<TaskDTO> findByUserEmail(String email) {
        log.debug("Запрос списка задач для пользователя | email={}", email);

        List<Task> tasks = taskRepository.findByUserEmail(email);

        log.info("Возвращено задач для пользователя | email={} | count={}", email, tasks.size());

        return tasks.stream()
                .map(taskMapper::toDto)
                .toList();
    }

    public TaskDTO save(String email, TaskCreateDTO taskDTO) {
        log.info("Создание новой задачи | email={} | taskName={}", email, taskDTO.taskName());

        User user = userService.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Попытка создать задачу для несуществующего пользователя | email={}", email);
                    return new UsernameNotFoundException("Пользователь не найден");
                });

        Task task = taskMapper.toEntity(taskDTO, user);
        task = taskRepository.save(task);

        log.info("Задача успешно создана | id={} | email={} | taskName={}",
                task.getId(), email, task.getTaskName());

        return taskMapper.toDto(task);
    }

    public TaskDTO partialUpdate(Long id, UserDetails userDetails, TaskPatchUpdateDTO updateDTO) {
        log.debug("Частичное обновление задачи | id={}", id);

        Task task = taskRepository.findTaskById(id)
                .orElseThrow(() -> {
                    log.warn("Попытка частичного обновления несуществующей задачи | id={}", id);
                    return new ResourceNotFoundException("Такой задачи нет!");
                });

        validateTaskOwner(task, userDetails);

        updateDTO.taskName().ifPresent(task::setTaskName);
        updateDTO.description().ifPresent(task::setDescription);
        updateDTO.status().ifPresent(task::setStatus);
        updateDTO.completedAt().ifPresent(task::setCompletedAt);

        task = taskRepository.save(task);

        log.info("Задача частично обновлена | id={} | taskName={}", id, task.getTaskName());

        return taskMapper.toDto(task);
    }

    public TaskDTO fullUpdate(Long id, UserDetails userDetails, TaskCreateDTO taskDTO) {
        log.debug("Полное обновление задачи | id={}", id);

        Task task = taskRepository.findTaskById(id)
                .orElseThrow(() -> {
                    log.warn("Попытка полного обновления несуществующей задачи | id={}", id);
                    return new ResourceNotFoundException("Такой задачи нет!");
                });

        validateTaskOwner(task, userDetails);

        task.setTaskName(taskDTO.taskName());
        task.setDescription(taskDTO.description());
        task.setCompletedAt(taskDTO.completedAt());
        task.setStatus(taskDTO.status());

        task = taskRepository.save(task);

        log.info("Задача полностью обновлена | id={} | taskName={} | status={}",
                id, task.getTaskName(), task.getStatus());

        return taskMapper.toDto(task);
    }

    public void delete(Long id, UserDetails userDetails) {
        log.info("Запрос на удаление задачи | id={}", id);

        Task task = taskRepository.findTaskById(id)
                .orElseThrow(() -> {
                    log.warn("Попытка удаления несуществующей задачи | id={}", id);
                    return new ResourceNotFoundException("Такой задачи нет!");
                });

        validateTaskOwner(task, userDetails);

        taskRepository.delete(task);

        log.info("Задача успешно удалена | id={} | taskName={}", id, task.getTaskName());
    }

    private void validateTaskOwner(Task task, UserDetails userDetails) {
        if (!task.getUser().getEmail().equals(userDetails.getUsername())) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Такой задачи не существует или вы пытаетесь изменить чужую задачу!"
            );
        }
    }
}
