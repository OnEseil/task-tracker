package task_tracker.auth.mapper;

import org.springframework.stereotype.Component;
import task_tracker.auth.dto.UserDTO;
import task_tracker.auth.entity.User;

@Component
public class UserMapper {

    public UserDTO toDto(User user) {
        return new UserDTO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole()
        );
    }
}
