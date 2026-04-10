package task_tracker.tasks.handler;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException.*;
import org.springframework.web.client.HttpServerErrorException;
import task_tracker.tasks.entity.ExceptionClass;
import task_tracker.tasks.exception.EmailAlreadyExistsException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ExceptionClass> handleEmailExceptions() {
        return new ResponseEntity<>(new ExceptionClass("Этот email уже существует!"), HttpStatus.CONFLICT);
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ExceptionClass> handleBadRequest(){
        return new ResponseEntity<>(new ExceptionClass("Неверный запрос!"), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ExceptionClass> handleResourceNotFound() {
        return new ResponseEntity<>(new ExceptionClass("Ресурс не найден!"), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(Unauthorized.class)
    public ResponseEntity<ExceptionClass> handUnauthorized(){
        return new ResponseEntity<>(new ExceptionClass("В заголовке отсутствует токен или он неправильный!")
                , HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(Forbidden.class)
    public ResponseEntity<ExceptionClass> handleForbidden(){
        return new ResponseEntity<>(new ExceptionClass("У пользователя недостаточно прав для доступа к этой странице!")
                , HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ExceptionClass> handleGeneric(Exception ex) {
        return new ResponseEntity<>(
                new ExceptionClass("Внутренняя ошибка сервера!"),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

}
