package com.asr.orderservice.web;

import com.asr.orderservice.domain.Order;
import com.asr.orderservice.domain.OrderService;
import com.asr.orderservice.exception.OrderNotFoundException;
import com.asr.orderservice.product.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static com.asr.orderservice.domain.OrderStatus.ACCEPTED;
import static com.asr.orderservice.domain.OrderStatus.CANCELLED;
import static com.asr.orderservice.domain.OrderStatus.REJECTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@WebFluxTest(OrderController.class)
class OrderControllerTests {
    private static final String ORDER_ROOT_API = "/orders";

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private OrderService orderService;

    @Test
    @DisplayName("get orders, should return all orders")
    void getAllOrders() {
        // Given
        var order1 = Order.of("64b7a0b7d9492771d6b7ab8a", "Name 1", 1.0, 1, ACCEPTED);
        var order2 = Order.of("64b7a0b7d9492771d6b7ab8b", "Name 2", 2.0, 2, ACCEPTED);
        given(orderService.findAllOrders())
                .willReturn(Flux.fromIterable(List.of(order1, order2)));

        // When + Then
        webTestClient
                .get()
                .uri(ORDER_ROOT_API)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Order.class).value(actualOrders ->
                        assertThat(actualOrders)
                                .hasSize(2)
                                .containsExactlyInAnyOrderElementsOf(List.of(order1, order2))
                );
    }

    @Test
    @DisplayName("get order when not exists, should return 404")
    void getOrder_whenNotExists_return404() {
        // Given
        var orderId = "64b514da498c2e6567f000a2";
        given(orderService.findByOrderId(orderId))
                .willReturn(Mono.error(new OrderNotFoundException(orderId)));

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
    @DisplayName("get order when exists, should return order")
    void getOrder_whenExists_returnOrder() {
        // Given
        var order = Order.of("64b13f81160f6f18fe1fdd49", "Name", 1.0, 1, ACCEPTED);
        var orderId = "64b514da498c2e6567f000a2";
        given(orderService.findByOrderId(orderId))
                .willReturn(Mono.just(order));

        // When + Then
        webTestClient
                .get()
                .uri(ORDER_ROOT_API + "/" + orderId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Order.class).value(actualOrder ->
                        assertThat(actualOrder).isNotNull()
                                .hasFieldOrPropertyWithValue("productName", "Name")
                                .hasFieldOrPropertyWithValue("productPrice", 1.0)
                                .hasFieldOrPropertyWithValue("quantity", 1)
                                .hasFieldOrPropertyWithValue("status", ACCEPTED)
                );
    }

    @Test
    @DisplayName("submit order, when product available, should accept order")
    void submitOrder_whenProductAvailable_thenAcceptOrder() {
        // Given
        var product = new Product("64b514da498c2e6567f000a2", "Name", 1.0, 1L);
        var orderRequest = new OrderRequest(product.id(), 1);
        var expectedOrder = OrderService.buildAcceptedOrder(product, orderRequest.quantity());
        given(orderService.submitOrder(product.id(), orderRequest.quantity()))
                .willReturn(Mono.just(expectedOrder));

        // When + Then
        webTestClient
                .post()
                .uri(ORDER_ROOT_API)
                .bodyValue(orderRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Order.class).value(actualOrder -> {
                    assertThat(actualOrder).isNotNull()
                            .extracting(Order::status)
                            .isEqualTo(ACCEPTED);
                });
    }

    @Test
    void submitOrder_whenProductNotAvailable_thenRejectOrder() {
        // Given
        var orderRequest = new OrderRequest("64b514da498c2e6567f000a2", 1);
        var expectedOrder = OrderService.buildRejectedOrder(orderRequest.productId(), orderRequest.quantity());
        given(orderService.submitOrder(orderRequest.productId(), orderRequest.quantity()))
                .willReturn(Mono.just(expectedOrder));

        // When + Then
        webTestClient
                .post()
                .uri(ORDER_ROOT_API)
                .bodyValue(orderRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Order.class).value(actualOrder -> {
                    assertThat(actualOrder).isNotNull();
                    assertThat(actualOrder.status()).isEqualTo(REJECTED);
                });
    }

    @Test
    @DisplayName("cancel order when not exists, should return 404")
    void cancelOrder_whenNotExists_return404() {
        // Given
        var orderId = "64b514da498c2e6567f000a2";
        given(orderService.cancelOrder(orderId))
                .willReturn(Mono.error(new OrderNotFoundException(orderId)));

        // When + Then
        webTestClient
                .put()
                .uri(ORDER_ROOT_API + "/" + orderId)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(String.class).value(message ->
                        assertThat(message)
                                .isEqualTo("Order with ID '" + orderId + "' was not found.")
                );
    }

    @Test
    @DisplayName("cancel order when exists, should cancel")
    void cancelOrder_whenExists_shouldCancel() {
        // Given
        var orderId = "64b514da498c2e6567f000a2";
        var cancelledOrder = Order.of("64b13f81160f6f18fe1fdd49", "Name", 1.0, 1, CANCELLED);
        given(orderService.cancelOrder(orderId))
                .willReturn(Mono.just(cancelledOrder));

        // When + Then
        webTestClient
                .put()
                .uri(ORDER_ROOT_API + "/" + orderId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Order.class).value(actualOrder ->
                        assertThat(actualOrder.status()).isEqualTo(CANCELLED)
                );
    }

}
