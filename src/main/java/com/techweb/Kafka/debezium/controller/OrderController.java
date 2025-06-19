package com.techweb.Kafka.debezium.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.techweb.Kafka.debezium.entity.Order;
import com.techweb.Kafka.debezium.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestParam String description) throws JsonProcessingException {
        Order order = orderService.createOrder(description);
        return ResponseEntity.ok(order);
    }
}
