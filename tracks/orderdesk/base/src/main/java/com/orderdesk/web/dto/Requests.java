package com.orderdesk.web.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class Requests {

    private Requests() {
    }

    public record CreateProduct(@NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{1,32}") String sku,
            @NotBlank @Size(max = 120) String name, @NotNull @DecimalMin("0.00") BigDecimal price,
            @Min(0) @Max(1_000_000) int initialStock) {
    }

    public record Restock(@Min(1) @Max(1_000_000) int quantity) {
    }

    public record Pay(@NotBlank String cardNumber) {
    }

    public record Refund(@NotNull BigDecimal amount) {
    }
}
