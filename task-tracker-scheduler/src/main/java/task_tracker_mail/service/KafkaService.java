package task_tracker_mail.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import task_tracker_mail.dto.DoneReminderDTO;
import task_tracker_mail.dto.InProgressReminderDTO;
import task_tracker_mail.dto.MixedReminderDTO;


@RequiredArgsConstructor
@Service
@Slf4j
public class KafkaService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    @Value(value = "${topic}")
    private String topicName;

    public void sendMixedTasksReminder(MixedReminderDTO send) {
        log.info("Отправка смешанного напоминания в Kafka | topic = {} | email = {}",
                topicName, send.email());

        kafkaTemplate.send(topicName, send);

        log.debug("Сообщение смешанного напоминания отправлено в Kafka | email = {}", send.email());
    }

    public void sendInProgressReminder(InProgressReminderDTO send) {
        log.info("Отправка напоминания о задачах в работе в Kafka | topic = {} | email = {}",
                topicName, send.email());

        kafkaTemplate.send(topicName, send);

        log.debug("Сообщение о задачах в работе отправлено в Kafka | email = {}", send.email());
    }

    public void sendDoneTasksReminder(DoneReminderDTO send) {
        log.info("Отправка напоминания о завершённых задачах в Kafka | topic = {} | email = {}",
                topicName, send.email());

        kafkaTemplate.send(topicName, send);

        log.debug("Сообщение о завершённых задачах отправлено в Kafka | email = {}", send.email());
    }
}