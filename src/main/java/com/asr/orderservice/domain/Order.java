package com.asr.orderservice.domain;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("orders")
public record Order(
        @Id
        String id,

        String productId,
        String productName,
        Double productPrice,
        Integer quantity,

        OrderStatus status,

        @CreatedDate
        Instant createdDate,

        @LastModifiedDate
        Instant lastModifiedDate,

        @Version
        int version
) {
    public static Order of(String productId, String productName, Double productPrice, Integer quantity, OrderStatus status) {
        return new Order(null, productId, productName, productPrice, quantity, status, null, null, 0);
    }
}
