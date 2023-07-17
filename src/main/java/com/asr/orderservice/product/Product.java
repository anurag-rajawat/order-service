package com.asr.orderservice.product;

// TODO: Expose a new endpoint in Catalog Service, returning Product objects modeled as this DTO
public record Product(
        String id,
        String name,
        Double price,
        Long units
) {
}
