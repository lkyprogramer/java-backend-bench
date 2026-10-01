package com.orderdesk.web.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PlaceOrderRequest(@NotBlank @Size(max = 64) String customerId,
        @NotNull @Size(min = 1, max = 20) List<@Valid Line> lines) {

    public record Line(@NotBlank @Size(max = 32) String sku, @Min(1) @Max(1000) int quantity) {
    }
}
