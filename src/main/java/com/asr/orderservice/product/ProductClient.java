package com.asr.orderservice.product;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

@Component
public class ProductClient {
    private static final String PRODUCTS_ROOT_API = "/products/";
    private final WebClient webClient;

    public ProductClient(WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<Product> getProduct(String productId) {
        return webClient
                .get()
                .uri(PRODUCTS_ROOT_API + productId)
                .retrieve()
                .bodyToMono(Product.class)
                .timeout(Duration.ofSeconds(3), Mono.empty())
                .onErrorResume(WebClientResponseException.NotFound.class,
                        exception -> Mono.empty())
                // If any error happens after the 3 retry attempts, catch the exception and return an empty object.
                .retryWhen(
                        Retry.backoff(3, Duration.ofMillis(100))
                ).onErrorResume(Exception.class, exception -> Mono.empty());
    }
}
