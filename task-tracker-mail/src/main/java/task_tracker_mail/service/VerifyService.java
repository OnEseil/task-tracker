package task_tracker_mail.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
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
@RequiredArgsConstructor
@Slf4j
public class VerifyService {

    private final JavaMailSender mailSender;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private final String emailFrom = "bmolkov2006@gmail.com";

    @KafkaListener(
            topics = "EMAIL_VERIFYING",
            groupId = "email-service-group"
    )
    public void sendVerifyMessage(VerifyEmailDTO email) {

        log.info("Получен запрос на отправку кода верификации | email = {}", email);

        try {
            String random = String.valueOf(new SecureRandom().nextInt(100_000, 1_000_000));
            log.debug("Сгенерирован код верификации | email = {} | code = {}", email, random);

            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setTo(email.email());
            mailMessage.setSubject("Verification code");
            mailMessage.setText(random);
            mailMessage.setFrom(emailFrom);

            mailSender.send(mailMessage);
            log.info("Письмо с кодом верификации успешно отправлено | email = {}", email);

            redisTemplate.opsForValue().set(email.email(), random, 10, TimeUnit.MINUTES);
            log.debug("Код верификации сохранён в Redis | email = {} | ttl = 10 мин", email);

        } catch (Exception e) {
            log.error("Ошибка при отправке письма верификации | email = {} | {}", email, e.getMessage(), e);
        }
    }
}