package task_tracker.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import task_tracker.auth.dto.*;
import task_tracker.auth.service.KafkaEmailService;
import task_tracker.auth.service.UserService;

import java.util.Objects;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication",
        description = "Операции регистрации, входа и верификации аккаунта")
public class AuthController {

    private final StringRedisTemplate redisTemplate;
    private final UserService userService;
    private final KafkaEmailService kafkaEmailService;

    @Operation(summary = "Регистрация пользователя", description = "Создает аккаунт и возвращает access/refresh токены")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Пользователь успешно зарегистрирован",
                    content = @Content(schema = @Schema(implementation = TokensDTO.class))),
            @ApiResponse(responseCode = "409", description = "Email уже существует",
                    content = @Content(schema = @Schema(implementation = String.class)))
    })
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid RegisterRequest register) {
        String email = register.email();
        log.info("Получен запрос на регистрацию | email = {}", email);

        if (userService.existsByEmail(email)) {
            log.warn("Попытка регистрации — email уже занят | email = {}", email);
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Такой email уже существует");
        }

        log.info("Отправляем письмо с верификацией | email = {}", email);
        try {
            kafkaEmailService.sendEmail(new VerifyEmailDTO(register.email()));
            log.info("Письмо с верификацией успешно отправлено в Kafka | email = {}", email);
        } catch (Exception e) {
            log.error("Не удалось отправить письмо верификации | email = {} | ошибка: {}",
                    email, e.getMessage(), e);
        }

        log.info("Пользователь успешно зарегистрирован (ожидает верификации) | email = {}", email);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.registerAndSave(register));
    }

    @Operation(summary = "Верификация аккаунта(сразу после регистрации)", description = "Подтверждает аккаунт по коду из email")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Аккаунт успешно верифицирован",
                    content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "400", description = "Код верификации неверный или истек",
                    content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера",
                    content = @Content(schema = @Schema(implementation = String.class)))
    })
    @PostMapping("/verifying")
    public ResponseEntity<?> verify(
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(hidden = true)
            HttpServletRequest httpServletRequest,
            @RequestBody @Valid VerifyDTO userCode) {

        String email = userDetails.getUsername();
        log.info("Получена попытка верификации | email = {}", email);

        String codeFromRedis = redisTemplate.opsForValue().get(email);

        if (codeFromRedis == null) {
            log.warn("Код верификации не найден или истёк | email = {}", email);
            return ResponseEntity.badRequest()
                    .body("Код верификации истёк или не найден");
        }

        if (Objects.equals(codeFromRedis, userCode.code())) {
            log.info("Код верификации верный | email = {}", email);

            try {
                userService.verify(httpServletRequest, email);
                log.info("Аккаунт успешно верифицирован | email = {}", email);
                return ResponseEntity.ok("Аккаунт верифицирован");
            } catch (Exception e) {
                log.error("Ошибка при верификации аккаунта | email = {} | {}",
                        email, e.getMessage(), e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Ошибка при верификации");
            }
        } else {
            log.warn("Неверный код верификации | email = {}", email);
            return ResponseEntity.badRequest()
                    .body("Неверный код верификации");
        }
    }

    @Operation(summary = "Вход в систему", description = "Аутентифицирует пользователя и возвращает новые токены")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешная аутентификация",
                    content = @Content(schema = @Schema(implementation = TokensDTO.class))),
            @ApiResponse(responseCode = "401", description = "Неверный пароль",
                    content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "422", description = "Пользователь не найден",
                    content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера",
                    content = @Content(schema = @Schema(implementation = String.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginRequest loginRequest) {
        String email = loginRequest.email();
        log.info("Попытка входа | email = {}", email);

        if (!userService.existsByEmail(email)) {
            log.warn("Попытка входа — пользователь не найден | email = {}", email);
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body("Пользователь с таким email не зарегистрирован");
        }

        try {
            var response = userService.login(loginRequest);
            log.info("Успешный вход | email = {}", email);
            return ResponseEntity.ok(response);
        } catch (BadCredentialsException e) {
            log.warn("Неудачный вход — неверный пароль | email = {}", email);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Неверный пароль");
        } catch (Exception e) {
            log.error("Ошибка при входе | email = {} | {}", email, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Ошибка сервера при попытке входа");
        }
    }

    @PostMapping("/refresh_token")
    @Operation(summary = "Обновление токенов пользователя")
    public ResponseEntity<?> refreshToken(
            HttpServletRequest httpServletRequest
    ) {
        return ResponseEntity.ok(userService.refreshToken(httpServletRequest));
    }

    @PostMapping("/logout")
    @Operation(summary = "Выход из системы")
    public ResponseEntity<?> logout(
            HttpServletRequest httpServletRequest
    ) {
        userService.logout(httpServletRequest);
        return ResponseEntity.ok("успешно!");
    }
}
