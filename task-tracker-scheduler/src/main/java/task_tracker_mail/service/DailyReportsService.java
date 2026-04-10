package task_tracker_mail.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import task_tracker_mail.dto.DoneReminderDTO;
import task_tracker_mail.dto.InProgressReminderDTO;
import task_tracker_mail.dto.MixedReminderDTO;
import task_tracker_mail.dto.TaskDTO;
import task_tracker_mail.entity.Status;
import task_tracker_mail.entity.Task;
import task_tracker_mail.entity.User;
import task_tracker_mail.repository.DailyReportsRepository;
import task_tracker_mail.repository.Users;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DailyReportsService {

    private static final Status DONE_REMINDER = Status.DONE;
    private static final Status IN_PROGRESS_REMINDER = Status.IN_PROGRESS;
    private final DailyReportsRepository dailyReportsRepository;
    private final Users usersRepository;
    private final KafkaService kafkaService;

    @Scheduled(cron = "0 0 0 * * *")
    public void sendDailyTaskReminders() {
        log.info("Запущена задача отправки ежедневных напоминаний о задачах");

        List<User> users = usersRepository.findAll();
        log.info("Найдено пользователей для обработки: {}", users.size());

        users.forEach(this::sendReminderForUser);

        log.info("Обработка напоминаний завершена (всего пользователей обработано: {})", users.size());
    }

    private void sendReminderForUser(User user) {
        String email = user.getEmail();
        Long userId = user.getId();

        log.debug("Обработка напоминания для пользователя | id={} | email={}", userId, email);

        long countInProgress = dailyReportsRepository
                .countByStatusAndUserId(IN_PROGRESS_REMINDER, userId);

        long countDone = dailyReportsRepository
                .countByStatusAndUserIdAndCompletedAt(DONE_REMINDER, userId, LocalDate.now().minusDays(1));

        log.debug("У пользователя | email={} | in progress: {} | done за вчера: {}",
                email, countInProgress, countDone);

        if (countInProgress == 0 && countDone == 0) {
            log.debug("Нет активных или завершённых задач — напоминание не отправляем | email={}", email);
            return;
        }

        List<TaskDTO> top5InProgress = convertTasks(
                dailyReportsRepository.findTop5ByStatusAndUserId(IN_PROGRESS_REMINDER, userId));

        List<TaskDTO> top5Done = convertTasks(
                dailyReportsRepository.findTop5ByStatusAndUserIdAndCompletedAt(
                        DONE_REMINDER, userId, LocalDate.now().minusDays(1)));

        if (countInProgress > 0 && countDone > 0) {
            log.info("Отправляем смешанное напоминание | email={} | in progress: {} | done: {}",
                    email, countInProgress, countDone);
            sendBothTypesEmail(
                    new MixedReminderDTO(email, top5InProgress, countInProgress, top5Done, countDone)
            );
        } else if (countDone > 0) {
            log.info("Отправляем напоминание только о завершённых задачах | email={} | count={}",
                    email, countDone);
            sendOnlyDoneEmail(
                    new DoneReminderDTO(email, top5Done, countDone)
            );
        } else if (countInProgress > 0) {
            log.info("Отправляем напоминание только о задачах в работе | email={} | count={}",
                    email, countInProgress);
            sendOnlyInProgressEmail(
                    new InProgressReminderDTO(email, top5InProgress, countInProgress)
            );
        }
    }

    private void sendOnlyInProgressEmail(InProgressReminderDTO dto) {
        log.debug("Отправка в Kafka: напоминание о задачах в работе | email={}", dto.email());
        kafkaService.sendInProgressReminder(dto);
    }

    private void sendOnlyDoneEmail(DoneReminderDTO dto) {
        log.debug("Отправка в Kafka: напоминание о завершённых задачах | email={}", dto.email());
        kafkaService.sendDoneTasksReminder(dto);
    }

    private void sendBothTypesEmail(MixedReminderDTO dto) {
        log.debug("Отправка в Kafka: смешанное напоминание | email={}", dto.email());
        kafkaService.sendMixedTasksReminder(dto);
    }

    private List<TaskDTO> convertTasks(List<Task> tasks) {
        log.trace("Конвертация списка задач в DTO | количество задач = {}", tasks.size());

        List<TaskDTO> dtos = tasks.stream()
                .map(task -> new TaskDTO(
                        task.getTaskName(),
                        task.getStatus(),
                        task.getDescription(),
                        task.getCompletedAt()
                ))
                .collect(Collectors.toList());

        log.trace("Конвертация завершена | получено DTO: {}", dtos.size());
        return dtos;
    }
}