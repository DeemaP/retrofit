package com.adas.retrofit.config;

import com.adas.retrofit.delegate.ProcessVariables;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.camunda.bpm.engine.task.IdentityLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Шлёт письмо-оповещение при создании (назначении) любой user task.
 * Адресат вычисляется по ролям задачи (candidateGroups): роль ENGINEER →
 * engineer@{domain}. Реальных пользователей в проекте нет, поэтому оповещаем
 * «почтовый ящик роли» — в демо это ловит Mailpit.
 *
 * <p>{@link JavaMailSender} опционален: если SMTP не сконфигурирован
 * (нет {@code SPRING_MAIL_HOST}), бин отсутствует и оповещения тихо отключаются.
 * Любая ошибка отправки логируется и НЕ роняет процесс.
 */
@Component("taskNotificationListener")
public class TaskNotificationListener implements TaskListener {

    private static final Logger log = LoggerFactory.getLogger(TaskNotificationListener.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String from;
    private final String recipientDomain;

    public TaskNotificationListener(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${notifications.from:no-reply@adas.local}") String from,
            @Value("${notifications.recipient-domain:adas.local}") String recipientDomain) {
        this.mailSenderProvider = mailSenderProvider;
        this.from = from;
        this.recipientDomain = recipientDomain;
    }

    @Override
    public void notify(DelegateTask task) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.debug("SMTP не сконфигурирован — оповещение по задаче '{}' пропущено", task.getName());
            return;
        }

        Set<String> roles = task.getCandidates().stream()
                .map(IdentityLink::getGroupId)
                .filter(g -> g != null && !g.isBlank())
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));

        if (roles.isEmpty()) {
            log.debug("У задачи '{}' нет candidateGroups — некому слать оповещение", task.getName());
            return;
        }

        String[] recipients = roles.stream()
                .map(role -> role.toLowerCase() + "@" + recipientDomain)
                .toArray(String[]::new);

        Object orderId = task.getVariable(ProcessVariables.ORDER_ID);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(recipients);
        message.setSubject("[ADAS] Новая задача: " + task.getName());
        message.setText(buildBody(task, roles, orderId));

        try {
            mailSender.send(message);
            log.info("Оповещение по задаче '{}' (заявка {}) отправлено: {}",
                    task.getName(), orderId, String.join(", ", recipients));
        } catch (Exception e) {
            // отправка почты не должна влиять на исполнение процесса
            log.warn("Не удалось отправить оповещение по задаче '{}': {}", task.getName(), e.getMessage());
        }
    }

    private String buildBody(DelegateTask task, Set<String> roles, Object orderId) {
        return """
                Назначена новая задача в процессе дооснащения ADAS.

                Задача:   %s
                Роль:     %s
                Заявка:   %s
                Task ID:  %s

                Откройте Camunda Tasklist или возьмите задачу через REST API
                (POST /api/v1/tasks/%s/complete).
                """.formatted(
                task.getName(),
                String.join(", ", roles),
                orderId == null ? "—" : orderId,
                task.getId(),
                task.getId());
    }
}