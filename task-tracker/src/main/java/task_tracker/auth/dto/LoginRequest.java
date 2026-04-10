package task_tracker.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotNull(message = "Email обязателен!")
        @Email(message = "Некорректный формат email")
        @Size(max = 30, message = "Размер email не должен превышать 30 символов!") String email,
        @NotNull(message = "Password обязателен!")
        @Size(max = 30, message = "Размер password не должен превышать 30 символов!") String password
) {
}
