package task_tracker.auth.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import task_tracker.auth.dto.LoginRequest;
import task_tracker.auth.dto.RegisterRequest;
import task_tracker.auth.dto.TokensDTO;
import task_tracker.auth.dto.UserDTO;
import task_tracker.auth.entity.Role;
import task_tracker.auth.entity.Token;
import task_tracker.auth.entity.User;
import task_tracker.auth.mapper.UserMapper;
import task_tracker.auth.repository.TokenRepository;
import task_tracker.auth.repository.UserRepository;
import task_tracker.auth.util.JwtUtil;

import java.util.Optional;

@RequiredArgsConstructor
@Service
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final UserDetailsService userDetailsService;
    private final UserMapper userMapper;

    public UserDTO findUserByEmail(String email) {
        User user = userRepository.findUserByEmail(email);
        return userMapper.toDto(user);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public TokensDTO registerAndSave(RegisterRequest request) {
        String email = request.email();

        log.info("Регистрация нового пользователя | email={}", email);

        if (userRepository.existsByEmail(email)) {
            log.warn("Попытка регистрации уже существующего email | email={}", email);
            throw new IllegalArgumentException("Пользователь с таким email уже существует");
        }

        User user = User.builder()
                .username(request.username())
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .role(Role.NON_VERIFY)
                .build();

        user = userRepository.save(user);
        log.info("Пользователь успешно создан | id={} | email={} | role={}",
                user.getId(), email, user.getRole());

        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

        Token token = Token.builder()
                .accessToken(jwtUtil.generateAccessToken(userDetails))
                .refreshToken(jwtUtil.generateRefreshToken(userDetails))
                .user(user)
                .build();

        tokenRepository.save(token);
        log.info("Токены созданы и сохранены | userId={} | email={}", user.getId(), email);

        return new TokensDTO(token.getAccessToken(), token.getRefreshToken());
    }

    public void verify(HttpServletRequest httpServletRequest, String email) {
        log.info("Попытка верификации аккаунта | email={}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Верификация не удалась: пользователь не найден | email={}", email);
                    return new UsernameNotFoundException("Пользователь с такой почтой не найден: " + email);
                });

        String requestToken = jwtUtil.extractToken(httpServletRequest);
        Token storedToken = tokenRepository.findTokenByUserId(user.getId());

        if (storedToken == null) {
            log.warn("Токен для пользователя не найден в БД | userId={} | email={}", user.getId(), email);
            throw new IllegalArgumentException("Токен не найден");
        }

        if (!requestToken.equals(storedToken.getAccessToken())) {
            log.warn("Токен верификации не совпадает | email={} | userId={}", email, user.getId());
            throw new IllegalArgumentException("Токен не совпадает с владельцем!");
        }

        user.setRole(Role.USER);
        userRepository.save(user);

        log.info("Аккаунт успешно верифицирован | email={} | newRole={}", email, user.getRole());
    }

    public TokensDTO login(LoginRequest loginRequest) {
        String email = loginRequest.email();
        log.info("Попытка входа | email={}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Неудачная попытка входа: пользователь не найден | email={}", email);
                    return new UsernameNotFoundException("Пользователь с такой почтой не найден: " + email);
                });

        boolean passwordMatches = passwordEncoder.matches(loginRequest.password(), user.getPassword());

        if (!passwordMatches) {
            log.warn("Неудачная попытка входа: неверный пароль | email={}", email);
            throw new BadCredentialsException("Логин или пароль не совпадают!");
        }

        log.debug("Успешная проверка пароля для пользователя | email={}", email);

        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

        Token oldToken = tokenRepository.findTokenByUserId(user.getId());
        if (oldToken != null) {
            tokenRepository.delete(oldToken);
            log.debug("Удалён предыдущий токен | userId={}", user.getId());
        }

        Token newToken = Token.builder()
                .accessToken(jwtUtil.generateAccessToken(userDetails))
                .refreshToken(jwtUtil.generateRefreshToken(userDetails))
                .user(user)
                .build();

        tokenRepository.save(newToken);

        log.info("Успешный логин | email={} | userId={} | new access token issued",
                email, user.getId());

        return new TokensDTO(newToken.getAccessToken(), newToken.getRefreshToken());
    }

    public TokensDTO refreshToken(HttpServletRequest httpServletRequest) {
        String token = jwtUtil.extractToken(httpServletRequest);
        String email = jwtUtil.extractUsername(token);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("No user found"));

        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

        Token oldToken = tokenRepository.findTokenByUserId(user.getId());

        if (oldToken != null) {
            tokenRepository.delete(oldToken);
            log.debug("Удалён предыдущий токен | userId={}", user.getId());
        }

        Token newToken = Token.builder()
                .accessToken(jwtUtil.generateAccessToken(userDetails))
                .refreshToken(jwtUtil.generateRefreshToken(userDetails))
                .user(user)
                .build();

        tokenRepository.save(newToken);

        return new TokensDTO(newToken.getAccessToken(), newToken.getRefreshToken());
    }

    public void logout(HttpServletRequest httpServletRequest) {
        String token = jwtUtil.extractToken(httpServletRequest);
        String email = jwtUtil.extractUsername(token);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("No user found"));

        userDetailsService.loadUserByUsername(email);

        Token oldToken = tokenRepository.findTokenByUserId(user.getId());

        if (oldToken != null) {
            tokenRepository.delete(oldToken);
            log.debug("Удалён предыдущий токен | userId={}", user.getId());
        }
    }
}
