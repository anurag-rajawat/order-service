package com.asr.orderservice.domain;

import com.asr.orderservice.config.DataConfig;
import com.asr.orderservice.product.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;

@DataMongoTest
@Testcontainers
@Import(DataConfig.class)
class OrderRepositoryTests {

    @Container
    private static final MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:6.0");

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ReactiveMongoTemplate reactiveMongoTemplate;

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Test
    @DisplayName("find by id when not exists, should return empty order")
    void findById_whenNotExists() {
        // Given
        var orderId = "64b7a0b7d9492771d6b7ab8a";

        // When
        var actualOrder = orderRepository.findById(orderId);

        // Then
        StepVerifier
                .create(actualOrder)
                .expectNextCount(0)
                .verifyComplete();
    }

    @Test
    @DisplayName("find by id, when exists, should return order")
    void findById_whenExists_returnOrder() {
        // Given
        var product = new Product("64b7a0b7d9492771d6b7ab8a", "Name", 1.0, 1L);
        var acceptedOrder = OrderService.buildAcceptedOrder(product, 1);
        var orderId = reactiveMongoTemplate.insert(acceptedOrder).block().id();

        // When
        var actualOrder = orderRepository.findById(orderId);

        // Then
        StepVerifier
                .create(actualOrder)
                .expectNextMatches(order -> order.id().equals(orderId) &&
                        order.productName().equals(product.name()) &&
                        order.productPrice().equals(product.price()) &&
                        order.status().equals(OrderStatus.ACCEPTED)
                ).verifyComplete();
    }
}
