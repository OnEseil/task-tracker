package task_tracker.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import task_tracker.auth.dto.UserDTO;
import task_tracker.auth.service.UserService;

@RequiredArgsConstructor
@RestController
@Slf4j
public class UserController {
    private final UserService userService;

    @GetMapping("/user")
    @Operation(summary = "Получение данных о пользователе")
    public ResponseEntity<?> getUser(
            @AuthenticationPrincipal UserDetails user) {
        log.info("User: {} requested", user.getUsername());
        return ResponseEntity.ok(userService.findUserByEmail(user.getUsername()));
    }
}
