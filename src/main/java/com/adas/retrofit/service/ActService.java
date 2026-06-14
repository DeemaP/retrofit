package com.adas.retrofit.service;

import com.adas.retrofit.entity.ComplianceAct;
import com.adas.retrofit.entity.Order;
import com.adas.retrofit.repository.ComplianceActRepository;
import com.adas.retrofit.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.Year;
import java.util.UUID;

/** Логика работы с актами о соответствии (ComplianceAct). */
@Service
public class ActService {

    private final OrderRepository orderRepository;
    private final ComplianceActRepository actRepository;

    public ActService(OrderRepository orderRepository, ComplianceActRepository actRepository) {
        this.orderRepository = orderRepository;
        this.actRepository = actRepository;
    }

    /**
     * Создаёт акт о соответствии для заявки. Идемпотентно: если акт уже создан
     * (например, повторный прогон делегата), возвращает существующий.
     */
    @Transactional
    public ComplianceAct createForOrder(UUID orderId) {
        return actRepository.findByOrderId(orderId).orElseGet(() -> {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new IllegalStateException("Order not found: " + orderId));

            ComplianceAct act = new ComplianceAct();
            act.setOrder(order);
            act.setDocumentNumber(generateDocumentNumber(order));
            act.setIssuedAt(Instant.now());
            act.setSummary("Дооснащение системой " + order.getRetrofitType()
                    + " для VIN " + order.getVehicle().getVin() + " выполнено.");
            return actRepository.save(act);
        });
    }

    private String generateDocumentNumber(Order order) {
        String shortId = order.getId().toString().substring(0, 8);
        return "ADAS-" + Year.now() + "-" + shortId;
    }
}