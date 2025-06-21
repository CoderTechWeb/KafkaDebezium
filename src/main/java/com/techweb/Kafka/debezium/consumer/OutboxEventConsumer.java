package com.techweb.Kafka.debezium.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    public void listen(String message, @Header(KafkaHeaders.RECEIVED_KEY) String key) throws JsonProcessingException {
        try {
            // Parse message if needed
            // Simulate processing logic here...
            System.out.println("Consumed: " + message);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(key);
            JsonNode payloadNode = root.get("payload");

            if (payloadNode == null || payloadNode.isNull()) {
                throw new IllegalArgumentException("Missing payload field in message: " + message);
            }

            String eventId = payloadNode.asText(); // this gives "903"
            System.out.println("📦 Aggregate ID: " + eventId);

            orderService.updateOutboxStatus(eventId, OutboxEvent.Status.PUBLISHED);

        } catch (Exception e) {
            System.out.println("Consumed: " + message);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(key);
            JsonNode payloadNode = root.get("payload");

            if (payloadNode == null || payloadNode.isNull()) {
                throw new IllegalArgumentException("Missing payload field in message: " + message);
            }

            String eventId = payloadNode.asText(); // this gives "903"
            System.out.println("📦 Aggregate ID: " + eventId);            orderService.updateOutboxStatus(eventId, OutboxEvent.Status.FAILED);
        }
    }

    private Long extractEventId(String message) {
        // Dummy logic - use actual parsing in real case
        return 1L;
    }
}
