
# 📦 Event-Driven Microservice using Outbox Pattern with Debezium (CDC)

## ✅ Components Used
- PostgreSQL (`orderdb`)
- Kafka
- Debezium Connect (with PostgreSQL CDC)
- Kafka Topic: `outbox.event.Order`
- Spring Boot App with `@KafkaListener`

---

## 🐳 Step 1: Docker Compose Setup

```yaml
version: '3.8'
services:
  zookeeper:
    image: confluentinc/cp-zookeeper:7.2.1
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
    ports:
      - "2181:2181"

  kafka:
    image: confluentinc/cp-kafka:7.2.1
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_INTERNAL:PLAINTEXT
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092,PLAINTEXT_INTERNAL://kafka:29092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"

  postgres:
    image: postgres:14
    ports:
      - "5432:5432"
    environment:
      POSTGRES_DB: orderdb
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres

  connect:
    image: debezium/connect:2.6
    depends_on:
      - kafka
      - postgres
    ports:
      - "8083:8083"
    environment:
      BOOTSTRAP_SERVERS: kafka:29092
      GROUP_ID: 1
      CONFIG_STORAGE_TOPIC: connect-configs
      OFFSET_STORAGE_TOPIC: connect-offsets
      STATUS_STORAGE_TOPIC: connect-statuses
      KEY_CONVERTER_SCHEMAS_ENABLE: "false"
      VALUE_CONVERTER_SCHEMAS_ENABLE: "false"
      KEY_CONVERTER: org.apache.kafka.connect.json.JsonConverter
      VALUE_CONVERTER: org.apache.kafka.connect.json.JsonConverter
      PLUGIN_PATH: /kafka/connect
```

---

## 🧑‍💻 Step 2: Register Debezium PostgreSQL Connector

```bash
curl -X POST http://localhost:8083/connectors   -H "Content-Type: application/json"   -d '{
    "name": "order-connector",
    "config": {
      "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
      "database.hostname": "postgres",
      "database.port": "5432",
      "database.user": "postgres",
      "database.password": "postgres",
      "database.dbname": "orderdb",
      "database.server.name": "orderdb",
      "plugin.name": "pgoutput",
      "table.include.list": "public.outbox_event",
      "tombstones.on.delete": "false",
      "topic.prefix": "outbox",
      "transforms": "outbox",
      "transforms.outbox.type": "io.debezium.transforms.outbox.EventRouter",
      "transforms.outbox.table.field.event.id": "id",
      "transforms.outbox.table.field.event.key": "aggregate_id",
      "transforms.outbox.table.field.event.payload": "payload",
      "transforms.outbox.table.field.event.type": "event_type",
      "transforms.outbox.table.field.event.timestamp": "created_at",
      "transforms.outbox.table.field.event.aggregate.type": "aggregate_type",
      "transforms.outbox.table.field.event.aggregate.id": "aggregate_id",
      "transforms.outbox.route.by.field": "aggregate_type"
    }
  }'
```
---
# 🎯 How Debezium Determines Kafka Topic and How to Use Custom Listeners

## 🧠 How Debezium Determines the Kafka Topic

Debezium reads changes from the database and pushes them to Kafka topics. The topic name is derived using the configuration in your connector:

### Key Configuration Properties:

```json
"topic.prefix": "outbox",
"transforms.outbox.route.by.field": "aggregate_type"
        
---
To add custom listeners, you can use the `transforms` configuration in your Debezium connector. The `EventRouter` transform allows you to route events to different topics based on a field value.

"transforms.outbox.route.topic.replacement": "custom-prefix.${aggregate_type}.v1"
```

---

## 📥 Step 4: Insert Data into `outbox_event` Table

```sql
INSERT INTO outbox_event 
(aggregate_id, aggregate_type, created_at, event_type, payload, status, updated_at)
VALUES ('1', 'Order', now(), 'ORDER_CREATED', '{"orderId":1}', 'NEW', now());
```

---

## 🧪 Step 5: Kafka Topic Verification

```bash
docker exec -it kafka /bin/bash

# Create topic if not already created
kafka-topics --bootstrap-server localhost:9092 --create   --topic outbox.event.Order --replication-factor 1 --partitions 1
```

---

## 🎯 Step 6: Spring Boot Kafka Consumer

```java
@Component
public class OutboxEventConsumer {
    @Autowired
    private OrderService orderService;

    @KafkaListener(topics = "outbox.event.Order", groupId = "order-consumer")
    public void listen(String message, @Header(KafkaHeaders.RECEIVED_KEY) String key) {
        try {
            System.out.println("Consumed: " + message + ", key: " + key);
            Long eventId = extractEventId(message);
            orderService.updateOutboxStatus(eventId, OutboxEvent.Status.PUBLISHED);
        } catch (Exception e) {
            Long eventId = extractEventId(message);
            orderService.updateOutboxStatus(eventId, OutboxEvent.Status.FAILED);
        }
    }

    private Long extractEventId(String message) {
        // Parse logic goes here
        return 1L;
    }
}
```

---

## 🧠 Troubleshooting Summary

| Issue | Fix |
|------|-----|
| `curl: connection reset by peer` | Ensure Debezium connect container is up |
| `topic.prefix missing` | Add `"topic.prefix": "outbox"` |
| `KafkaHeaders.RECEIVED_MESSAGE_KEY` error | Use `KafkaHeaders.RECEIVED_KEY` |
| Debezium sees no changes | Check `table.include.list`, WAL, and outbox schema |

---

## 📂 Output

This is the complete markdown guide used for setting up and running a CDC-based outbox pattern using Debezium, Kafka, PostgreSQL, and Spring Boot.
# KafkaDebezium

# 🔄 Debezium vs Poller-Publisher: Which is Better for the Outbox Pattern?

When implementing the [Outbox Pattern](https://microservices.io/patterns/data/transactional-outbox.html) in a microservices architecture, there are two main strategies for publishing events to a message broker like Kafka:

- ✅ **Debezium-based CDC (Change Data Capture)**
- 🔁 **Poller-Publisher pattern**

---

## ✅ What is Debezium (CDC)?

**Debezium** is a distributed open-source platform for CDC. It listens to database transaction logs (WAL in PostgreSQL, binlog in MySQL) and pushes changes to Kafka in near real-time.

### How It Works:
1. Inserts/updates into the `outbox_event` table are captured directly from DB logs.
2. Debezium connector reads changes and transforms them using `EventRouter`.
3. Pushes events to a topic like `outbox.event.Order`.

---

## 🔁 What is Poller-Publisher?

This is a manual implementation where:
1. A scheduled task (poller) reads `NEW` events from the `outbox_event` table at intervals.
2. The app publishes those events to Kafka/RabbitMQ.
3. Updates event status (e.g., to `PUBLISHED` or `FAILED`).

---

## 🔍 Comparison Table

| Feature                        | Debezium (CDC)                             | Poller-Publisher                          |
|-------------------------------|--------------------------------------------|-------------------------------------------|
| **Event Delivery Latency**    | ⚡ Near real-time                           | 🕒 Delay based on poll interval            |
| **Implementation**            | Minimal code, just configure connector     | Requires code for polling, publishing     |
| **Resilience**                | Built-in retry, offset tracking            | Needs manual retry handling               |
| **Outbox Status Updates**     | Requires separate listener in service      | Done as part of the polling logic         |
| **Failure Handling**          | Robust (offset tracking, retry, DLQ)       | Error-prone unless handled carefully      |
| **Scalability**               | High, runs outside app                     | Limited by app resources                  |
| **Operational Overhead**      | Requires managing Debezium + Kafka Connect | Simpler if already using scheduled jobs   |
| **Custom Filtering**          | Supported via `transforms`                 | Fully customizable in code                |
| **Dependency on Kafka**       | Strong (Kafka-based architecture)          | Can work with any broker (Kafka, RabbitMQ)|

---

## ✅ When to Use Debezium

- You want **real-time** event streaming with minimal effort.
- You have Kafka and Debezium in your ecosystem.
- You prefer **low-latency, scalable** solutions.
- You want to keep **business logic decoupled** from infrastructure concerns.

---

## 🔁 When to Use Poller-Publisher

- You **do not use Kafka or Debezium**.
- You want **fine-grained control** over event generation.
- Your system already relies on scheduled jobs or batch processing.
- You need to **publish to multiple brokers** (e.g., Kafka + RabbitMQ).

---

## 🧠 Conclusion

| Use Case                          | Recommended Approach     |
|----------------------------------|--------------------------|
| Low-latency and scalable systems | ✅ Debezium (CDC)         |
| Legacy systems without Kafka     | 🔁 Poller-Publisher       |
| Simple setups or quick PoCs      | 🔁 Poller-Publisher       |
| Event-driven microservices       | ✅ Debezium (CDC)         |

For modern, production-grade, event-driven architectures, **Debezium-based CDC is preferred** due to its efficiency, low latency, and robustness.

---
