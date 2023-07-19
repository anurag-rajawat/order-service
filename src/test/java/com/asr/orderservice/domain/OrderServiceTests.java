package com.asr.orderservice.domain;

import com.asr.orderservice.exception.OrderNotFoundException;
import com.asr.orderservice.product.Product;
import com.asr.orderservice.product.ProductClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static com.asr.orderservice.domain.OrderStatus.ACCEPTED;
import static com.asr.orderservice.domain.OrderStatus.CANCELLED;
import static com.asr.orderservice.domain.OrderStatus.REJECTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class OrderServiceTests {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductClient productClient;

    @InjectMocks
    private OrderService orderService;

    @Test
    void buildRejectedOrder() {
        // Given
        var productId = "64b7a0b7d9492771d6b7ab8a";

        // When
        var rejectedOrder = OrderService.buildRejectedOrder(productId, 1);

        // Then
        assertThat(rejectedOrder.status()).isEqualTo(REJECTED);
    }

    @Test
    void buildAcceptedOrder() {
        // Given
        var product = new Product("64b7a0b7d9492771d6b7ab8a", "Name", 1.0, 1L);

        // When
        var acceptedOrder = OrderService.buildAcceptedOrder(product, 1);

        // Then
        assertThat(acceptedOrder)
                .hasFieldOrPropertyWithValue("status", ACCEPTED)
                .hasFieldOrPropertyWithValue("productName", product.name())
                .hasFieldOrPropertyWithValue("productPrice", product.price())
                .hasFieldOrPropertyWithValue("quantity", 1);
    }

    @Test
    @DisplayName("find all orders, should return all orders")
    void findAllOrders() {
        // Given
        var order1 = Order.of("64b7a0b7d9492771d6b7ab8a", "Name 1", 1.0, 1, ACCEPTED);
        var order2 = Order.of("64b7a0b7d9492771d6b7ab8b", "Name 2", 2.0, 2, ACCEPTED);
        given(orderRepository.findAll())
                .willReturn(Flux.fromIterable(List.of(order1, order2)));

        // When
        var orders = orderService.findAllOrders();

        // Then
        StepVerifier.create(orders)
                .expectNext(order1)
                .expectNext(order2)
                .verifyComplete();
    }

    @Test
    @DisplayName("find by order id when not exists, should throw exception")
    void findByOrderId_whenNotExists_shouldThrowException() {
        // Given
        var orderId = "64b13f81160f6f18fe1fdd49";
        given(orderRepository.findById(orderId))
                .willReturn(Mono.empty());

        // When
        var actualOrder = orderService.findByOrderId(orderId);

        // Then
        StepVerifier
                .create(actualOrder)
                .expectErrorMatches(err -> err instanceof OrderNotFoundException &&
                        err.getMessage().equals("Order with ID '" + orderId + "' was not found."))
                .verify();
    }

    @Test
    @DisplayName("find by order id when exists, should return that order")
    void findByOrderId_whenExists_shouldReturnOrder() {
        // Given
        var orderId = "64b13f81160f6f18fe1fdd49";
        var expectedOrder = Order.of("64b13f81160f6f18fe1fdd49", "Name", 1.0, 1, ACCEPTED);
        given(orderRepository.findById(orderId))
                .willReturn(Mono.just(expectedOrder));

        // When
        var actualOrder = orderService.findByOrderId(orderId);

        // Then
        StepVerifier.create(actualOrder)
                .expectNext(expectedOrder)
                .verifyComplete();
    }

    @Test
    @DisplayName("submit order should accept the order")
    void submitOrder_whenNotExists_shouldAccept() {
        // Given
        var product = new Product("64b13f81160f6f18fe1fdd49", "Product name", 1.0, 1L);
        given(productClient.getProduct(product.id()))
                .willReturn(Mono.just(product));

        var acceptedOrder = OrderService.buildAcceptedOrder(product, 1);
        given(orderRepository.save(acceptedOrder))
                .willReturn(Mono.just(acceptedOrder));

        // When
        var actualOrder = orderService.submitOrder(product.id(), 1);

        // Then
        StepVerifier.create(actualOrder)
                .expectNext(acceptedOrder)
                .verifyComplete();
    }

    @Test
    @DisplayName("submit order when quantity is greater than product units, should reject the order")
    void submitOrder_whenQuantityGreaterThanUnits_shouldReject() {
        // Given
        var product = new Product("64b13f81160f6f18fe1fdd49", "Product name", 1.0, 1L);
        given(productClient.getProduct(product.id()))
                .willReturn(Mono.just(product));

        var rejectedOrder = OrderService.buildRejectedOrder(product.id(), 10);
        given(orderRepository.save(rejectedOrder))
                .willReturn(Mono.just(rejectedOrder));

        // When
        var actualOrder = orderService.submitOrder(product.id(), 10);

        // Then
        StepVerifier.create(actualOrder)
                .expectNext(rejectedOrder)
                .verifyComplete();
    }

    @Test
    @DisplayName("cancel order when exists, should cancel order")
    void cancelOrder_whenExists_cancelOrder() {
        // Given
        var orderId = "64b13f81160f6f18fe1fdd49";
        var product = new Product("64b13f81160f6f18fe1fdd49", "Product name", 1.0, 1L);

        var acceptedOrder = OrderService.buildAcceptedOrder(product, 1);
        given(orderRepository.findById(orderId))
                .willReturn(Mono.just(acceptedOrder));

        var cancelledOrder = Order.of(acceptedOrder.productId(), acceptedOrder.productName(),
                acceptedOrder.productPrice(), acceptedOrder.quantity(), CANCELLED);
        given(orderRepository.save(cancelledOrder))
                .willReturn(Mono.just(cancelledOrder));

        // When
        var actualOrder = orderService.cancelOrder(orderId);

        // Then
        StepVerifier.create(actualOrder)
                .expectNext(cancelledOrder)
                .verifyComplete();
    }

    @Test
    @DisplayName("cancel order when not exists, should throw exception")
    void cancelOrder_whenNotExists_shouldThrowException() {
        // Given
        var orderId = "64b13f81160f6f18fe1fdd49";
        given(orderRepository.findById(orderId))
                .willReturn(Mono.empty());

        // When
        var actualOrder = orderService.cancelOrder(orderId);

        // Then
        StepVerifier.create(actualOrder)
                .expectErrorMatches(err -> err instanceof OrderNotFoundException &&
                        err.getMessage().equals("Order with ID '" + orderId + "' was not found."))
                .verify();
    }
}
