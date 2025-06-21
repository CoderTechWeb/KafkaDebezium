package com.techweb.Kafka.debezium.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techweb.Kafka.debezium.entity.Order;
import com.techweb.Kafka.debezium.entity.OutboxEvent;
import com.techweb.Kafka.debezium.repo.OrderRepository;
import com.techweb.Kafka.debezium.repo.OutboxEventRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@AllArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;

    private final OutboxEventRepository outboxEventRepository;

    private final ObjectMapper objectMapper;

    @Transactional
    public Order createOrder(String description) throws JsonProcessingException {
        Order order = new Order();
        order.setDescription(description);
        order.setCreatedAt(LocalDateTime.now());
        order = orderRepository.save(order);

        OutboxEvent event = new OutboxEvent();
        event.setAggregateType("Order");
        event.setAggregateId(order.getId().toString());
        event.setEventType("OrderCreated");
        event.setPayload(objectMapper.writeValueAsString(order));
        event.setStatus(OutboxEvent.Status.NEW);
        outboxEventRepository.save(event);

        return order;
    }

    @Transactional
    public void updateOutboxStatus(String aggregateId, OutboxEvent.Status status) {
        outboxEventRepository.updateStatusByAggregateId(aggregateId, status);

    }
}
