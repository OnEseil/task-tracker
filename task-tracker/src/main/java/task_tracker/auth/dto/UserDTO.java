package task_tracker.auth.dto;

import task_tracker.auth.entity.Role;

public record UserDTO(
        Long id,
        String username,
        String email,
        Role role
) {
}
