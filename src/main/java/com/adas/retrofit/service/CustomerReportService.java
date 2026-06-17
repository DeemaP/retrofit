package com.adas.retrofit.service;

import com.adas.retrofit.entity.ComplianceAct;
import com.adas.retrofit.entity.Customer;
import com.adas.retrofit.entity.Order;
import com.adas.retrofit.entity.Part;
import com.adas.retrofit.entity.Vehicle;
import com.adas.retrofit.repository.CalibrationRecordRepository;
import com.adas.retrofit.repository.ComplianceActRepository;
import com.adas.retrofit.repository.PartRepository;
import com.adas.retrofit.repository.ProgrammingRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Отправляет клиенту итоговый отчёт по дооснащению на e-mail после успешной выдачи
 * автомобиля. Для демо письма ловит Mailpit. {@link JavaMailSender} опционален: без
 * настроенного SMTP отчёт тихо не отправляется; ошибки отправки логируются и не
 * влияют на процесс.
 */
@Service
public class CustomerReportService {

    private static final Logger log = LoggerFactory.getLogger(CustomerReportService.class);

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.systemDefault());

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final ComplianceActRepository actRepository;
    private final PartRepository partRepository;
    private final ProgrammingRecordRepository programmingRepository;
    private final CalibrationRecordRepository calibrationRepository;
    private final String from;

    public CustomerReportService(ObjectProvider<JavaMailSender> mailSenderProvider,
                                 ComplianceActRepository actRepository,
                                 PartRepository partRepository,
                                 ProgrammingRecordRepository programmingRepository,
                                 CalibrationRecordRepository calibrationRepository,
                                 @Value("${notifications.from:no-reply@adas.local}") String from) {
        this.mailSenderProvider = mailSenderProvider;
        this.actRepository = actRepository;
        this.partRepository = partRepository;
        this.programmingRepository = programmingRepository;
        this.calibrationRepository = calibrationRepository;
        this.from = from;
    }

    /**
     * Формирует и отправляет клиенту отчёт по заявке. Вызывается на завершении процесса.
     *
     * @param dismantled true, если процесс завершился демонтажом системы и возвратом
     *                   автомобиля в исходное состояние (дооснащение не выполнено)
     */
    @Transactional(readOnly = true)
    public void sendCompletionReport(Order order, boolean dismantled) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.debug("SMTP не сконфигурирован — отчёт клиенту по заявке {} не отправлен", order.getId());
            return;
        }

        Customer customer = order.getCustomer();
        String email = customer == null ? null : customer.getEmail();
        if (email == null || email.isBlank()) {
            log.debug("У клиента заявки {} нет e-mail — отчёт не отправлен", order.getId());
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject(dismantled
                ? "Ваш автомобиль готов к выдаче — система демонтирована"
                : "Ваш автомобиль готов — отчёт о дооснащении ADAS");
        message.setText(dismantled ? buildDismantleReport(order, customer) : buildReport(order, customer));

        try {
            mailSender.send(message);
            log.info("Отчёт по заявке {} отправлен клиенту на {} (демонтаж={})", order.getId(), email, dismantled);
        } catch (Exception e) {
            log.warn("Не удалось отправить отчёт клиенту по заявке {}: {}", order.getId(), e.getMessage());
        }
    }

    private String buildReport(Order order, Customer customer) {
        UUID orderId = order.getId();
        Vehicle vehicle = order.getVehicle();
        ComplianceAct act = actRepository.findByOrderId(orderId).orElse(null);
        List<Part> parts = partRepository.findByOrderId(orderId);
        long programmingCount = programmingRepository.findByOrderIdOrderByCreatedAtAsc(orderId).size();
        long calibrationCount = calibrationRepository.findByOrderIdOrderByCreatedAtAsc(orderId).size();

        StringBuilder sb = new StringBuilder();
        sb.append("Здравствуйте, ").append(customer.getFullName()).append("!\n\n");
        sb.append("Работы по дооснащению Вашего автомобиля системой активной безопасности ")
                .append(order.getRetrofitType()).append(" успешно завершены.\n\n");

        sb.append("Автомобиль:\n");
        if (vehicle != null) {
            sb.append("  Модель:    ").append(nvl(vehicle.getModel()))
                    .append(vehicle.getYear() == null ? "" : ", " + vehicle.getYear()).append('\n');
            sb.append("  VIN:       ").append(nvl(vehicle.getVin())).append('\n');
            if (vehicle.getLicensePlate() != null) {
                sb.append("  Госномер:  ").append(vehicle.getLicensePlate()).append('\n');
            }
        }
        sb.append('\n');

        sb.append("Установленные компоненты");
        if (parts.isEmpty()) {
            sb.append(": —\n");
        } else {
            sb.append(" (").append(parts.size()).append("):\n");
            for (Part p : parts) {
                sb.append("  • ").append(p.getName());
                if (p.getArticle() != null && !p.getArticle().isBlank()) {
                    sb.append(" [").append(p.getArticle()).append(']');
                }
                if (p.getQuantity() > 1) {
                    sb.append(" ×").append(p.getQuantity());
                }
                sb.append('\n');
            }
        }
        sb.append('\n');

        sb.append("Выполнено операций программирования (кодирование): ").append(programmingCount).append('\n');
        sb.append("Выполнено калибровок: ").append(calibrationCount).append("\n\n");

        if (act != null) {
            sb.append("Документ:\n");
            sb.append("  Акт о соответствии №: ").append(act.getDocumentNumber()).append('\n');
            if (act.getIssuedAt() != null) {
                sb.append("  Дата выдачи:          ").append(DATE_FMT.format(act.getIssuedAt())).append('\n');
            }
            if (act.getSummary() != null) {
                sb.append("  Заключение:           ").append(act.getSummary()).append('\n');
            }
            sb.append('\n');
        }

        sb.append("Спасибо, что выбрали наш сервисный центр!\n");
        sb.append("Это письмо сформировано автоматически.\n");
        return sb.toString();
    }

    /** Отчёт для исхода «демонтаж/возврат»: дооснащение не выполнено, авто возвращено в исходное состояние. */
    private String buildDismantleReport(Order order, Customer customer) {
        UUID orderId = order.getId();
        Vehicle vehicle = order.getVehicle();
        ComplianceAct act = actRepository.findByOrderId(orderId).orElse(null);

        StringBuilder sb = new StringBuilder();
        sb.append("Здравствуйте, ").append(customer.getFullName()).append("!\n\n");
        sb.append("К сожалению, завершить дооснащение Вашего автомобиля системой ")
                .append(order.getRetrofitType())
                .append(" не удалось. Установленное оборудование демонтировано, ")
                .append("автомобиль приведён в исходное (заводское) состояние и готов к выдаче.\n\n");

        sb.append("Автомобиль:\n");
        if (vehicle != null) {
            sb.append("  Модель:    ").append(nvl(vehicle.getModel()))
                    .append(vehicle.getYear() == null ? "" : ", " + vehicle.getYear()).append('\n');
            sb.append("  VIN:       ").append(nvl(vehicle.getVin())).append('\n');
            if (vehicle.getLicensePlate() != null) {
                sb.append("  Госномер:  ").append(vehicle.getLicensePlate()).append('\n');
            }
        }
        sb.append('\n');

        sb.append("Состояние: система демонтирована, дополнительные компоненты в автомобиле не установлены.\n\n");

        if (act != null) {
            sb.append("Документ:\n");
            sb.append("  Акт №:       ").append(act.getDocumentNumber()).append('\n');
            if (act.getIssuedAt() != null) {
                sb.append("  Дата выдачи: ").append(DATE_FMT.format(act.getIssuedAt())).append('\n');
            }
            sb.append('\n');
        }

        sb.append("Приносим извинения за неудобства. По вопросам обращайтесь в наш сервисный центр.\n");
        sb.append("Это письмо сформировано автоматически.\n");
        return sb.toString();
    }

    private static String nvl(String s) {
        return s == null ? "—" : s;
    }
}
