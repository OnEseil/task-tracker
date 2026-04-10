package task_tracker.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VerifyDTO(
        @NotBlank(message = "Код верификации обязателен!")
        @Size(min = 6, max = 6, message = "Код верификации не должен быть длиннее 6 символов!") String code
) {
}
