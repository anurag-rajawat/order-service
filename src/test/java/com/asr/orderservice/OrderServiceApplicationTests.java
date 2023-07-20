package com.asr.orderservice;

import com.asr.orderservice.domain.Order;
import com.asr.orderservice.domain.OrderRepository;
import com.asr.orderservice.domain.OrderService;
import com.asr.orderservice.product.Product;
import com.asr.orderservice.product.ProductClient;
import com.asr.orderservice.web.OrderRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Mono;

import java.util.List;

import static com.asr.orderservice.domain.OrderStatus.ACCEPTED;
import static com.asr.orderservice.domain.OrderStatus.CANCELLED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@Testcontainers
@ActiveProfiles("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderServiceApplicationTests {
    private static final String ORDER_ROOT_API = "/orders";

    @Container
    private static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:6.0");

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private OrderRepository orderRepository;

    @MockBean
    private ProductClient productClient;

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll().block();
    }

    @Test
    @DisplayName("when get orders, then return all orders")
    void whenGetOrders_thenOrdersReturned() {
        // Given
        var order1 = Order.of("64b7a0b7d9492771d6b7ab8a", "Name 1", 1.0, 1, ACCEPTED);
        var order2 = Order.of("64b7a0b7d9492771d6b7ab8b", "Name 2", 2.0, 2, ACCEPTED);
        orderRepository.saveAll(List.of(order1, order2)).blockLast();

        // When + Then
        webTestClient
                .get()
                .uri(ORDER_ROOT_API)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Order.class).value(actualOrders ->
                        assertThat(actualOrders).hasSize(2)
                );

    }

    @Test
    @DisplayName("when get order by order id and order exists, then order should be returned")
    void whenGetOrder_orderExists_thenOrderReturned() {
        // Given
        var product = new Product("64b7a0b7d9492771d6b7ab8a", "Name", 1.0, 1L);
        var order = Order.of(product.id(), product.name(), product.price(), 1, ACCEPTED);
        var orderId = orderRepository.save(order).block().id();

        // When + Then
        webTestClient
                .get()
                .uri(ORDER_ROOT_API + "/" + orderId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Order.class).value(actualOrder -> {
                    assertOrder(actualOrder, order);
                });
    }

    @Test
    @DisplayName("when get order by order id and order not exists, then 404 should be returned")
    void whenGetOrder_orderNotExists_then404Returned() {
        // Given
        var orderId = "64b7a0b7d9492771d6b7ab8a";

        // When + Then
        webTestClient
                .get()
                .uri(ORDER_ROOT_API + "/" + orderId)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(String.class).value(message ->
                        assertThat(message)
                                .isEqualTo("Order with ID '" + orderId + "' was not found.")
                );
    }

    @Test
    @DisplayName("when submit order and product exists, then order should be submitted")
    void whenSubmitOrder_productExists_thenOrderSubmitted() {
        // Given
        var product = new Product("64b7da8dc8b40a76594270a2", "Name", 1.0, 1L);
        given(productClient.getProduct(product.id())).willReturn(Mono.just(product));

        var orderRequest = new OrderRequest(product.id(), 1);
        var expectedOrder = Order.of(product.id(), product.name(), product.price(), 1, ACCEPTED);

        // When + Then
        webTestClient.post()
                .uri(ORDER_ROOT_API)
                .bodyValue(orderRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Order.class).value(actualOrder ->
                        assertOrder(actualOrder, expectedOrder)
                );
    }

    @Test
    @DisplayName("when submit order and product not exists, then order should be rejected")
    void whenSubmitOrder_productNotExists_thenOrderRejected() {
        // Given
        var product = new Product("64b7da8dc8b40a76594270a2", "Name", 1.0, 1L);
        given(productClient.getProduct(product.id())).willReturn(Mono.empty());

        var orderRequest = new OrderRequest(product.id(), 1);
        var expectedOrder = OrderService.buildRejectedOrder(orderRequest.productId(), orderRequest.quantity());

        // When + Then
        webTestClient
                .post()
                .uri(ORDER_ROOT_API)
                .bodyValue(orderRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Order.class).value(actualOrder ->
                        assertOrder(actualOrder, expectedOrder)
                );
    }

    @Test
    @DisplayName("when submit order with quantity more than available product units, then order should be rejected")
    void whenSubmitOrder_withQuantityMoreThanProductUnits_thenOrderRejected() {
        // Given
        var product = new Product("64b7da8dc8b40a76594270a2", "Name", 1.0, 1L);
        given(productClient.getProduct(product.id())).willReturn(Mono.empty());

        var orderRequest = new OrderRequest(product.id(), 5);
        var expectedOrder = OrderService.buildRejectedOrder(orderRequest.productId(), orderRequest.quantity());

        // When + Then
        webTestClient
                .post()
                .uri(ORDER_ROOT_API)
                .bodyValue(orderRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Order.class).value(actualOrder ->
                        assertOrder(actualOrder, expectedOrder)
                );
    }

    @Test
    @DisplayName("when cancel order and order exists, then order should be cancelled")
    void whenCancelOrder_orderExists_thenOrderCancelled() {
        // Given
        var product = new Product("64b7a0b7d9492771d6b7ab8a", "Name", 1.0, 1L);
        var order = Order.of(product.id(), product.name(), product.price(), 1, ACCEPTED);
        var orderId = orderRepository.save(order).block().id();
        var expectedOrder = Order.of(order.productId(), order.productName(), order.productPrice(), order.quantity(), CANCELLED);

        // When + Then
        webTestClient
                .put()
                .uri(ORDER_ROOT_API + "/" + orderId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Order.class).value(actualOrder ->
                        assertOrder(actualOrder, expectedOrder)
                );
    }

    @Test
    @DisplayName("when cancel order and order not exists, then 404 should be returned")
    void whenCancelOrder_orderNotExists_then404Returned() {
        // Given
        var orderId = "64b7a0b7d9492771d6b7ab8a";

        // When + Then
        webTestClient
                .put()
                .uri(ORDER_ROOT_API + "/" + orderId)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(String.class).value(message ->
                        assertThat(message).isEqualTo("Order with ID '" + orderId + "' was not found."));
    }

    private void assertOrder(Order actualOrder, Order expectedOrder) {
        assertThat(actualOrder).isNotNull()
                .hasFieldOrPropertyWithValue("productName", expectedOrder.productName())
                .hasFieldOrPropertyWithValue("productPrice", expectedOrder.productPrice())
                .hasFieldOrPropertyWithValue("quantity", expectedOrder.quantity())
                .hasFieldOrPropertyWithValue("status", expectedOrder.status());
    }
}
