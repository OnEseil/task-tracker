package task_tracker_mail.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import task_tracker_mail.dto.*;
import tools.jackson.databind.ObjectMapper;

import java.security.SecureRandom;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@KafkaListener(
        topics = "EMAIL_SENDING_TASKS",
        groupId = "reports-service-group"
)
@RequiredArgsConstructor
public class TaskService {

    private final JavaMailSender mailSender;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${emailFromSend}")
    private String emailFrom;


    @KafkaHandler
    public void handleMixedReminder(MixedReminderDTO dto) {
        String email = dto.email();
        log.info("Отправка смешанного отчёта по задачам | email = {} | выполнено: {} | в работе: {}",
                email, dto.completedCount(), dto.uncompletedCount());

        SimpleMailMessage mailMessage = createBaseMailMessage();
        mailMessage.setTo(email);
        mailMessage.setText("Некоторые ваши выполненные задачи за сегодня: " + formatTaskNames(dto.completed()) +
                " всего за день выполнено: " + dto.completedCount() +
                "\nТакже незавершённые задачи: " + formatTaskNames(dto.uncompleted()) +
                " осталось выполнить: " + dto.uncompletedCount());

        mailSender.send(mailMessage);
        log.info("Смешанный отчёт успешно отправлен | email = {}", email);
    }

    @KafkaHandler
    public void handleDoneReminder(DoneReminderDTO dto) {
        String email = dto.email();
        log.info("Отправка отчёта о выполненных задачах | email = {} | выполнено: {}",
                email, dto.completedCount());

        SimpleMailMessage mailMessage = createBaseMailMessage();
        mailMessage.setTo(email);
        mailMessage.setText("Некоторые ваши выполненные задачи за сегодня: " + formatTaskNames(dto.completed()) +
                " всего за день выполнено: " + dto.completedCount());

        mailSender.send(mailMessage);
        log.info("Отчёт о выполненных задачах успешно отправлен | email = {}", email);
    }

    @KafkaHandler
    public void handleInProgressReminder(InProgressReminderDTO dto) {
        String email = dto.email();
        log.info("Отправка отчёта о незавершённых задачах | email = {} | осталось: {}",
                email, dto.inProgressTasksCount());

        SimpleMailMessage mailMessage = createBaseMailMessage();
        mailMessage.setTo(email);
        mailMessage.setText("Некоторые ваши оставшиеся незавершённые задачи: " +
                formatTaskNames(dto.inProgressTasksList()) +
                " , всего осталось выполнить: " + dto.inProgressTasksCount());

        mailSender.send(mailMessage);
        log.info("Отчёт о незавершённых задачах успешно отправлен | email = {}", email);
    }

    @KafkaHandler(isDefault = true)
    public void handleUnknownMessage(String message) {
        log.warn("Получено сообщение неизвестного формата — отчёт не отправлен | длина = {}", message.length());
    }

    private SimpleMailMessage createBaseMailMessage() {
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setSubject("Today's task");
        mailMessage.setFrom(emailFrom);
        return mailMessage;
    }

    private StringBuilder formatTaskNames(List<TaskDTO> taskDTOList) {
        StringBuilder sb = new StringBuilder();
        for (TaskDTO taskDTO : taskDTOList) {
            sb.append(taskDTO.taskName()).append(", ");
        }
        log.trace("Сформирован список названий задач | количество = {}", taskDTOList.size());
        return sb;
    }
}