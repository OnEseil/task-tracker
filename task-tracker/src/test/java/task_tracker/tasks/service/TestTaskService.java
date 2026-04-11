package task_tracker.tasks.service;

import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.server.ResponseStatusException;
import task_tracker.auth.entity.Role;
import task_tracker.auth.entity.User;
import task_tracker.auth.service.UserService;
import task_tracker.tasks.dto.TaskCreateDTO;
import task_tracker.tasks.dto.TaskDTO;
import task_tracker.tasks.dto.TaskPatchUpdateDTO;
import task_tracker.tasks.entity.Status;
import task_tracker.tasks.entity.Task;
import task_tracker.tasks.mapper.TaskMapper;
import task_tracker.tasks.repository.TaskRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TestTaskService {

    private long id;
    private UserDetails userDetails;
    private Task task1;
    private Task task2;
    private String email;
    private TaskCreateDTO taskCreateDTO;
    private User user;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserService userService;

    @Spy
    private TaskMapper taskMapper;

    @InjectMocks
    private TaskService taskService;

    @BeforeEach
    void prepareCommonTasks() {
        email = "test@mail.ru";

        user = new User(1L, "test username", email, null, Role.USER, null, null);

        task1 = Task.builder()
                .id(1L)
                .taskName("test 1")
                .status(Status.TO_DO)
                .description("test description 1")
                .completedAt(LocalDate.parse("2026-03-20"))
                .user(user)
                .build();

        task2 = Task.builder()
                .id(2L)
                .taskName("test 2")
                .status(Status.IN_PROGRESS)
                .description("test description 2")
                .completedAt(LocalDate.parse("2026-03-21"))
                .user(user)
                .build();

        taskCreateDTO = new TaskCreateDTO(
                "test 1",
                Status.TO_DO,
                "test description 1",
                LocalDate.parse("2026-03-20")
        );

        id = 1L;
        userDetails = mock(UserDetails.class);
    }

    @Nested
    @DisplayName("findByUserEmail")
    class FindByUserEmailTests {

        @Test
        @DisplayName("должен вернуть корректно преобразованные DTO")
        void testWhenTasksExist() {
            when(taskRepository.findByUserEmail(email)).thenReturn(List.of(task1, task2));

            List<TaskDTO> result = taskService.findByUserEmail(email);

            assertThat(result)
                    .hasSize(2)
                    .extracting(TaskDTO::id, TaskDTO::taskName, TaskDTO::status, TaskDTO::description, TaskDTO::completedAt)
                    .containsExactlyInAnyOrder(
                            org.assertj.core.groups.Tuple.tuple(1L, "test 1", Status.TO_DO, "test description 1", LocalDate.parse("2026-03-20")),
                            org.assertj.core.groups.Tuple.tuple(2L, "test 2", Status.IN_PROGRESS, "test description 2", LocalDate.parse("2026-03-21"))
                    );

            verify(taskRepository).findByUserEmail(email);
            verifyNoMoreInteractions(taskRepository, userService);
        }

        @Test
        @DisplayName("должен вернуть пустой список")
        void testWhenTasksNotExist() {
            when(taskRepository.findByUserEmail(email)).thenReturn(List.of());

            List<TaskDTO> result = taskService.findByUserEmail(email);

            assertThat(result).isEmpty();

            verify(taskRepository).findByUserEmail(email);
            verifyNoMoreInteractions(taskRepository, userService);
        }

        @Test
        @DisplayName("должен корректно обработать null email")
        void shouldHandleNullEmail() {
            when(taskRepository.findByUserEmail(null)).thenReturn(List.of());

            List<TaskDTO> result = taskService.findByUserEmail(null);

            assertThat(result).isEmpty();
            verify(taskRepository).findByUserEmail(null);
            verifyNoMoreInteractions(taskRepository, userService);
        }
    }

    @Nested
    @DisplayName("save")
    class SaveTests {

        @Test
        @DisplayName("должен создать задачу и вернуть корректный DTO")
        void saveWhenTaskExist() {
            when(userService.findByEmail(email)).thenReturn(Optional.of(user));
            when(taskRepository.save(any(Task.class)))
                    .thenAnswer(invocation -> {
                        Task savedTask = invocation.getArgument(0);
                        savedTask.setId(3L);
                        return savedTask;
                    });

            TaskDTO result = taskService.save(email, taskCreateDTO);

            assertAll(
                    () -> assertEquals(3L, result.id()),
                    () -> assertEquals(taskCreateDTO.taskName(), result.taskName()),
                    () -> assertEquals(taskCreateDTO.status(), result.status()),
                    () -> assertEquals(taskCreateDTO.description(), result.description()),
                    () -> assertEquals(taskCreateDTO.completedAt(), result.completedAt())
            );

            verify(userService).findByEmail(email);
            verify(taskRepository).save(any(Task.class));
            verifyNoMoreInteractions(userService, taskRepository);
        }

        @Test
        @DisplayName("возвращает ошибку, если пользователь не найден")
        void saveWhenUserNotFound() {
            when(userService.findByEmail(email)).thenReturn(Optional.empty());

            UsernameNotFoundException ex = assertThrows(
                    UsernameNotFoundException.class,
                    () -> taskService.save(email, taskCreateDTO)
            );

            assertEquals("Пользователь не найден", ex.getMessage());

            verify(userService).findByEmail(email);
            verifyNoInteractions(taskRepository);
            verifyNoMoreInteractions(userService);
        }
    }

    @Nested
    @DisplayName("partialUpdate")
    class PartialUpdateTest {

        @Test
        @DisplayName("должен частично обновить задачу и вернуть обновлённый DTO")
        void partiallyUpdateTaskName() {
            Long taskId = 1L;
            String newTaskName = "test task name";

            TaskPatchUpdateDTO patch = new TaskPatchUpdateDTO(
                    Optional.of(newTaskName),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty()
            );

            when(taskRepository.findTaskById(taskId)).thenReturn(Optional.of(task1));
            when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

            UserDetails currentUser = mock(UserDetails.class);
            when(currentUser.getUsername()).thenReturn(email);

            TaskDTO result = taskService.partialUpdate(taskId, currentUser, patch);

            assertAll(
                    () -> assertEquals(taskId, result.id()),
                    () -> assertEquals(newTaskName, result.taskName()),
                    () -> assertEquals(task1.getStatus(), result.status()),
                    () -> assertEquals(task1.getDescription(), result.description()),
                    () -> assertEquals(task1.getCompletedAt(), result.completedAt())
            );

            verify(taskRepository).findTaskById(taskId);
            verify(taskRepository).save(task1);
            verifyNoMoreInteractions(taskRepository, userService);
            assertEquals(newTaskName, task1.getTaskName());
        }

        @Test
        @DisplayName("задача не найдена")
        void whenNotFoundTaskTest() {
            when(taskRepository.findTaskById(id)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(
                    ResourceNotFoundException.class,
                    () -> taskService.partialUpdate(
                            id,
                            userDetails,
                            new TaskPatchUpdateDTO(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty())
                    )
            );

            assertEquals("Такой задачи нет!", ex.getMessage());

            verify(taskRepository).findTaskById(id);
            verifyNoMoreInteractions(taskRepository, userService);
        }

        @Test
        @DisplayName("username не совпадает")
        void whenUsernameNotMatchTest() {
            when(taskRepository.findTaskById(id)).thenReturn(Optional.of(task1));
            when(userDetails.getUsername()).thenReturn("not-match@mail.ru");

            ResponseStatusException ex = assertThrows(
                    ResponseStatusException.class,
                    () -> taskService.partialUpdate(
                            id,
                            userDetails,
                            new TaskPatchUpdateDTO(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty())
                    )
            );

            assertAll(
                    () -> assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode()),
                    () -> assertEquals("Такой задачи не существует или вы пытаетесь изменить чужую задачу!", ex.getReason())
            );

            verify(taskRepository).findTaskById(id);
            verifyNoMoreInteractions(taskRepository, userService);
        }
    }

    @Nested
    @DisplayName("fullUpdate")
    class FullUpdateTest {

        @Test
        @DisplayName("успешное полное обновление")
        void fullUpdateTest() {
            when(taskRepository.findTaskById(id)).thenReturn(Optional.of(task1));
            when(userDetails.getUsername()).thenReturn(email);
            when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

            TaskDTO result = taskService.fullUpdate(id, userDetails, taskCreateDTO);

            assertAll(
                    () -> assertEquals(id, result.id()),
                    () -> assertEquals(taskCreateDTO.taskName(), result.taskName()),
                    () -> assertEquals(taskCreateDTO.status(), result.status()),
                    () -> assertEquals(taskCreateDTO.description(), result.description()),
                    () -> assertEquals(taskCreateDTO.completedAt(), result.completedAt())
            );

            assertAll(
                    () -> assertEquals(taskCreateDTO.taskName(), task1.getTaskName()),
                    () -> assertEquals(taskCreateDTO.status(), task1.getStatus()),
                    () -> assertEquals(taskCreateDTO.description(), task1.getDescription()),
                    () -> assertEquals(taskCreateDTO.completedAt(), task1.getCompletedAt())
            );

            verify(taskRepository).findTaskById(id);
            verify(taskRepository).save(task1);
            verifyNoMoreInteractions(taskRepository, userService);
        }

        @Test
        @DisplayName("не найдена задача")
        void notFoundTask() {
            when(taskRepository.findTaskById(id)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(
                    ResourceNotFoundException.class,
                    () -> taskService.fullUpdate(id, userDetails, taskCreateDTO)
            );

            assertEquals("Такой задачи нет!", ex.getMessage());

            verify(taskRepository).findTaskById(id);
            verifyNoMoreInteractions(taskRepository, userService);
        }

        @Test
        @DisplayName("username не совпадает")
        void usernameNotMatchTest() {
            when(taskRepository.findTaskById(id)).thenReturn(Optional.of(task1));
            when(userDetails.getUsername()).thenReturn("not-match@mail.ru");

            ResponseStatusException ex = assertThrows(
                    ResponseStatusException.class,
                    () -> taskService.fullUpdate(id, userDetails, taskCreateDTO)
            );

            assertAll(
                    () -> assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode()),
                    () -> assertEquals("Такой задачи не существует или вы пытаетесь изменить чужую задачу!", ex.getReason())
            );

            verify(taskRepository).findTaskById(id);
            verify(taskRepository, never()).save(any(Task.class));
            verifyNoMoreInteractions(taskRepository, userService);
        }
    }

    @Nested
    @DisplayName("delete")
    class DeleteTest {

        @Test
        @DisplayName("успешное удаление задачи")
        void deleteTaskTest() {
            when(userDetails.getUsername()).thenReturn(email);
            when(taskRepository.findTaskById(id)).thenReturn(Optional.of(task1));

            taskService.delete(id, userDetails);

            verify(taskRepository).findTaskById(id);
            verify(taskRepository).delete(task1);
            verifyNoMoreInteractions(taskRepository, userService);
        }

        @Test
        @DisplayName("задача не была найдена")
        void deleteWhenTaskNotFoundTest() {
            when(taskRepository.findTaskById(id)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(
                    ResourceNotFoundException.class,
                    () -> taskService.delete(id, userDetails)
            );

            assertEquals("Такой задачи нет!", ex.getMessage());

            verify(taskRepository).findTaskById(id);
            verifyNoMoreInteractions(taskRepository, userService);
        }

        @Test
        @DisplayName("не совпадает username")
        void deleteWhenUsernameNotMatchTest() {
            when(userDetails.getUsername()).thenReturn("not-match@mail.ru");
            when(taskRepository.findTaskById(id)).thenReturn(Optional.of(task1));

            ResponseStatusException ex = assertThrows(
                    ResponseStatusException.class,
                    () -> taskService.delete(id, userDetails)
            );

            assertAll(
                    () -> assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode()),
                    () -> assertEquals("Такой задачи не существует или вы пытаетесь изменить чужую задачу!", ex.getReason())
            );

            verify(taskRepository).findTaskById(id);
            verify(taskRepository, never()).delete(any(Task.class));
            verifyNoMoreInteractions(taskRepository, userService);
        }
    }
}
