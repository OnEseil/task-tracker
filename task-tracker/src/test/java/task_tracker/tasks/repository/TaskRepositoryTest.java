package task_tracker.tasks.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import task_tracker.auth.entity.Role;
import task_tracker.auth.entity.User;
import task_tracker.tasks.entity.Status;
import task_tracker.tasks.entity.Task;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class TaskRepositoryTest {
    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findByUserEmail() {
        User user = new User(null, "Test", "test@example.com", "dasda", Role.USER, null, null);
        entityManager.persist(user);
        Task task1 = new Task(null, "test task 1", Status.TO_DO, "asdasda", LocalDate.parse("2026-03-24"), user);
        Task task2 = new Task(null, "test task 2", Status.TO_DO, "asdasda", LocalDate.parse("2026-03-24"), user);
        entityManager.persist(task1);
        entityManager.persist(task2);
        entityManager.flush();
        String email = "test@example.com";

        List<Task> tasks = taskRepository.findByUserEmail(email);

        assertThat(tasks).hasSize(2)
                .extracting(Task::getTaskName)
                .containsExactlyInAnyOrder("test task 1", "test task 2");

        assertThat(tasks)
                .allSatisfy(task -> {
                    assertThat(task.getUser()).isNotNull();
                    assertThat(task.getUser().getEmail()).isEqualTo("test@example.com");
                });

        assertThat(tasks)
                .extracting(t -> t.getUser().getUsername())
                .containsOnly("Test");
    }

    @Test
    void findByUserEmailWhenListEmpty() {
        String email = "test@mail.ru";

        List<Task> tasks = taskRepository.findByUserEmail(email);

        assertThat(tasks).isEmpty();
    }

    @Test
    void findTaskByIdTest() {
        User user = new User(null, "test@example.com", "Test", "dasda", Role.USER, null, null);
        entityManager.persist(user);
        Task task = new Task(null, "test task", Status.TO_DO, "asdasda", LocalDate.parse("2026-03-24"), user);
        entityManager.persist(task);
        entityManager.flush();

        Optional<Task> found = taskRepository.findTaskById(task.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getTaskName()).isEqualTo("test task");
    }
}
