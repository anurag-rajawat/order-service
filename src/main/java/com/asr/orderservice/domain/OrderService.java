package com.asr.orderservice.domain;

import com.asr.orderservice.exception.OrderNotFoundException;
import com.asr.orderservice.product.Product;
import com.asr.orderservice.product.ProductClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final ProductClient productClient;

    public OrderService(OrderRepository orderRepository, ProductClient productClient) {
        this.orderRepository = orderRepository;
        this.productClient = productClient;
    }

    public static Order buildRejectedOrder(String productId, int quantity) {
        return Order.of(productId, null, null, quantity, OrderStatus.REJECTED);
    }

    public static Order buildAcceptedOrder(Product product, int quantity) {
        return Order.of(product.id(), product.name(), product.price(), quantity, OrderStatus.ACCEPTED);
    }

    public Flux<Order> findAllOrders() {
        return orderRepository.findAll();
    }

    public Mono<Order> findByOrderId(String orderId) {
        return orderRepository.findById(orderId)
                .switchIfEmpty(Mono.error(new OrderNotFoundException(orderId)));
    }

    public Mono<Order> submitOrder(String productId, int quantity) {
        return productClient
                .getProduct(productId)
                .filter(product -> product.units() >= quantity)
                .map(product -> buildAcceptedOrder(product, quantity))
                // TODO: Integrate with payment service and after successful payment decrease the number of units of products
//                .doOnSuccess(product -> updateProductUnits(productId, quantity))
                .defaultIfEmpty(
                        buildRejectedOrder(productId, quantity)
                ).flatMap(orderRepository::save);
    }

    // TODO: Integrate with payment service and decrease units of products
    public Mono<Order> cancelOrder(String orderId) {
        return orderRepository.findById(orderId)
                .switchIfEmpty(Mono.error(new OrderNotFoundException(orderId)))
                .flatMap(existingOrder -> {
                    var orderToUpdate = new Order(
                            existingOrder.id(),
                            existingOrder.productId(),
                            existingOrder.productName(),
                            existingOrder.productPrice(),
                            existingOrder.quantity(),
                            OrderStatus.CANCELLED,
                            existingOrder.createdDate(),
                            existingOrder.lastModifiedDate(),
                            existingOrder.version()
                    );
                    return orderRepository.save(orderToUpdate);
                });
    }
}
