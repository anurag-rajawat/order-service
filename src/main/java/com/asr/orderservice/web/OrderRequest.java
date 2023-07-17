package com.asr.orderservice.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OrderRequest(
        @NotBlank(message = "Product ID must be defined.")
        String productId,

        @NotNull(message = "Product quantity must be defined.")
        @Min(value = 1, message = "You must order at least 1 item.")
        @Max(value = 100, message = "You cannot order more than 100 items.")
        Integer quantity
) {
}
