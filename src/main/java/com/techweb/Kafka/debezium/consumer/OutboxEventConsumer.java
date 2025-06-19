package com.techweb.Kafka.debezium.consumer;

import com.techweb.Kafka.debezium.entity.OutboxEvent;
import com.techweb.Kafka.debezium.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
public class OutboxEventConsumer {
    @Autowired
    private OrderService orderService;

    @KafkaListener(topics = "outbox.event.Order", groupId = "order-consumer")
    public void listen(String message, @Header(KafkaHeaders.RECEIVED_KEY) String key) {
        try {
            // Parse message if needed
            // Simulate processing logic here...
            System.out.println("Consumed: " + message);

            // Simulate updating status to PUBLISHED (you would extract event ID properly)
            // Assume message contains eventId as part of payload
            Long eventId = extractEventId(message);
            orderService.updateOutboxStatus(eventId, OutboxEvent.Status.PUBLISHED);

        } catch (Exception e) {
            Long eventId = extractEventId(message);
            orderService.updateOutboxStatus(eventId, OutboxEvent.Status.FAILED);
        }
    }

    private Long extractEventId(String message) {
        // Dummy logic - use actual parsing in real case
        return 1L;
    }
}
