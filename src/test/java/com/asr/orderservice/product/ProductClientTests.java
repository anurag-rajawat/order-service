package com.asr.orderservice.product;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;

class ProductClientTests {
    private MockWebServer mockWebServer;
    private ProductClient productClient;

    @BeforeEach
    void setUp() throws IOException {
        this.mockWebServer = new MockWebServer();
        this.mockWebServer.start();
        var webClient = WebClient.builder()
                .baseUrl(mockWebServer.url("/").uri().toString())
                .build();
        this.productClient = new ProductClient(webClient);
    }

    @Test
    void whenProductExists_thenReturnProduct() {
        // Given
        var productId = "64b514da498c2e6567f000a2";
        var mockResponse = new MockResponse()
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("""
                        {
                        "id": "%s",
                        "name": "Name",
                        "price": 1,
                        "units": 1
                        }
                        """.formatted(productId));
        mockWebServer.enqueue(mockResponse);

        // When
        Mono<Product> product = productClient.getProduct(productId);

        // Then
        StepVerifier
                .create(product)
                .expectNextMatches(p -> p.id().equals(productId) && p.name().equals("Name") &&
                        p.price().equals(1.0) && p.units().equals(1L)
                ).verifyComplete();
    }
}
