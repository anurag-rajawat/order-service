package com.asr.orderservice.web;

import com.asr.orderservice.domain.Order;
import com.asr.orderservice.domain.OrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public Flux<Order> getOrders() {
        return orderService.findAllOrders();
    }

    @GetMapping("{id}")
    public Mono<Order> getOrder(@PathVariable String id) {
        return orderService.findByOrderId(id);
    }

    @PostMapping
    public Mono<Order> submitOrder(@RequestBody @Valid OrderRequest orderRequest) {
        return orderService.submitOrder(orderRequest.productId(), orderRequest.quantity());
    }

//    TODO: Rethink PUT or PATCH
    @PutMapping("{id}")
    public Mono<Order> cancelOrder(@PathVariable String id) {
        return orderService.cancelOrder(id);
    }
}
