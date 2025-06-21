package com.techweb.Kafka.debezium.repo;

import com.techweb.Kafka.debezium.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
    Optional<Object> findByAggregateId(String aggregateId);

    @Modifying
    @Query("""
    UPDATE OutboxEvent e
    SET e.status = :status, e.updatedAt = CURRENT_TIMESTAMP
    WHERE e.aggregateId = :aggregateId
""")
    void updateStatusByAggregateId(@Param("aggregateId") String aggregateId,
                                   @Param("status") OutboxEvent.Status status);
}
