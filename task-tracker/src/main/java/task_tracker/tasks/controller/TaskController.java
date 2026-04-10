package task_tracker.tasks.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import task_tracker.tasks.dto.TaskCreateDTO;
import task_tracker.tasks.dto.TaskDTO;
import task_tracker.tasks.dto.TaskPatchUpdateDTO;
import task_tracker.tasks.service.TaskService;

@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Tasks", description = "Task CRUD operations")
@SecurityRequirement(name = "bearerAuth")
public class TaskController {
    private final TaskService taskService;

    @Operation(summary = "Получить все задачи пользователя")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Возвращён список всех задач",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = TaskDTO.class))))
    })
    @GetMapping("/tasks")
    public ResponseEntity<?> getAllUsersTasks(
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserDetails user) {
        log.info("User requested all tasks | email = {}", user.getUsername());

        return ResponseEntity.status(HttpStatus.OK)
                .body(taskService.findByUserEmail(user.getUsername()));
    }

    @Operation(summary = "Создать задачу")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Задача создана",
                    content = @Content(schema = @Schema(implementation = TaskDTO.class)))
    })
    @PostMapping("/task")
    public ResponseEntity<?> createTask(
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserDetails user,
            @RequestBody @Valid TaskCreateDTO taskDTO) {
        log.info("User creates task | email = {} | taskName = {}", user.getUsername(), taskDTO.taskName());

        TaskDTO task = taskService.save(user.getUsername(), taskDTO);

        return ResponseEntity.status(HttpStatus.CREATED).body(task);
    }

    @Operation(summary = "Обновление полей задачи")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Задача обновлена",
                    content = @Content(schema = @Schema(implementation = TaskDTO.class))),
            @ApiResponse(responseCode = "404", description = "Задача не найдена")
    })
    @PatchMapping("/tasks/{id}")
    public ResponseEntity<?> changeField(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid TaskPatchUpdateDTO updateDTO) {
        log.info("Partial task update | id = {} | payload = {}", id, updateDTO);

        TaskDTO updatedTask = taskService.partialUpdate(id, userDetails, updateDTO);

        log.info("Task partially updated | id = {}", id);

        return ResponseEntity.ok(updatedTask);
    }

    @Operation(summary = "Обновление всех полей задачи")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Задача обновлена",
                    content = @Content(schema = @Schema(implementation = TaskDTO.class))),
            @ApiResponse(responseCode = "404", description = "Задача не найдена")
    })
    @PutMapping("/tasks/{id}")
    public ResponseEntity<?> changeAllFields(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid TaskCreateDTO taskDTO) {
        log.info("Full task update | id = {} | payload = {}", id, taskDTO);

        TaskDTO updatedTask = taskService.fullUpdate(id, userDetails, taskDTO);

        log.info("Task fully updated | id = {}", id);

        return ResponseEntity.ok(updatedTask);
    }

    @Operation(summary = "Удаление задачи")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Задача удалена",
                    content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "404", description = "Задача не найдена")
    })
    @DeleteMapping("/tasks/{id}")
    public ResponseEntity<?> deleteTask(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        log.info("Delete task | id = {}", id);

        taskService.delete(id, userDetails);

        log.info("Task deleted | id = {}", id);

        return ResponseEntity.ok("success!");
    }
}
