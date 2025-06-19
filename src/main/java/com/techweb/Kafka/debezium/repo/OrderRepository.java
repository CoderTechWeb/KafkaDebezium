package com.techweb.Kafka.debezium.repo;

import com.techweb.Kafka.debezium.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
