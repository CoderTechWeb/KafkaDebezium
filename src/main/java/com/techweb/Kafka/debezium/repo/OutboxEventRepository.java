package com.techweb.Kafka.debezium.repo;

import com.techweb.Kafka.debezium.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
}
