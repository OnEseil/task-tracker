package task_tracker.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import task_tracker.auth.dto.VerifyEmailDTO;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaEmailService {
    @Value("${topic}")
    private String kafkaTopic;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public void sendEmail(VerifyEmailDTO email) {
        log.info("Отправка email в Kafka → {}", email.email());
        kafkaTemplate.send(kafkaTopic, email.email());
    }

}
