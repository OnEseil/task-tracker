package task_tracker.tasks.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import task_tracker.auth.entity.Role;
import task_tracker.auth.entity.User;
import task_tracker.auth.repository.UserRepository;
import task_tracker.auth.service.UserService;
import task_tracker.tasks.dto.TaskDTO;
import task_tracker.tasks.entity.Status;
import task_tracker.tasks.entity.Task;
import task_tracker.tasks.repository.TaskRepository;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@DataJpaTest
@Import(TaskService.class)
public class TaskServiceIT {
    @Autowired
    private TaskService taskService;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private UserService userService;

    @Nested
    @DisplayName("findByUserEmail")
    class FindByUserEmail {
        @Test
        @DisplayName("успешный возврат данных в дто формате")
        void findByUserEmail() {
            String email = "test@mail.ru";
            User user = User.builder()
                    .username("test")
                    .email(email)
                    .role(Role.USER)
                    .password("password")
                    .build();

            entityManager.persist(user);
            entityManager.flush();

            Task task1 = Task.builder()
                    .taskName("Купить молоко")
                    .status(Status.TO_DO)
                    .description("Взять 3.2%")
                    .completedAt(LocalDate.parse("2026-04-01"))
                    .user(user)
                    .build();

            Task task2 = Task.builder()
                    .taskName("Сдать проект")
                    .status(Status.IN_PROGRESS)
                    .description("До пятницы")
                    .user(user)
                    .completedAt(LocalDate.parse("2026-03-01"))
                    .build();

            taskRepository.saveAll(List.of(task1, task2));
            entityManager.flush();

            List<TaskDTO> result = taskService.findByUserEmail(email);

            assertThat(result)
                    .hasSize(2)
                    .extracting(TaskDTO::taskName, TaskDTO::status, TaskDTO::completedAt)
                    .containsExactlyInAnyOrder(
                            tuple("Купить молоко", Status.TO_DO, LocalDate.parse("2026-04-01")),
                            tuple("Сдать проект", Status.IN_PROGRESS, LocalDate.parse("2026-03-01"))
                    );
            assertThat(result)
                    .allSatisfy(dto -> {
                        assertThat(dto.id()).isNotNull();
                        assertThat(dto.taskName()).isNotBlank();
                    });
        }
    }
}
