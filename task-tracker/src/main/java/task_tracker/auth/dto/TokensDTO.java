package task_tracker.auth.dto;

public record TokensDTO(
        String accessToken,
        String refreshToken
) {
}
