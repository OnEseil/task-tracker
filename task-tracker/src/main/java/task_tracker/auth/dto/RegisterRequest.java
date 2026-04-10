package task_tracker.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotNull(message = "Username обязателен!")
        @Size(min = 4, max = 30, message = "Размер username должен быть от 4 до 30 символов!") String username,
        @NotNull(message = "Email обязателен!")
        @Email(message = "Некорректный формат email")
        @Size(max = 30, message = "Размер email не должен превышать 30 символов!") String email,
        @NotNull(message = "Password обязателен!")
        @Size(min = 4, max = 30, message = "Размер password должен быть от 4 до 30 символов!") String password
) {
}
