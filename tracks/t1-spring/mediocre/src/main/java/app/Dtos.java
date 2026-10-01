package app;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public class Dtos {
    public record CreateItem(@NotNull @Pattern(regexp = "[A-Za-z0-9_-]{1,32}") String sku,
                             @NotNull @Min(0) @Max(1000000000) Long quantity) {
    }

    public record LineDto(@NotNull @Pattern(regexp = "[A-Za-z0-9_-]{1,32}") String sku,
                          @NotNull @Min(1) @Max(1000000000) Long quantity) {
    }

    public record CreateReservation(@NotNull @Pattern(regexp = "[A-Za-z0-9_-]{1,64}") String requestId,
                                    @NotNull @Size(min = 1, max = 20) List<@Valid LineDto> lines,
                                    @NotNull @Min(1) @Max(86400000) Long ttlMillis) {
    }

    public record Advance(@NotNull @Min(0) @Max(86400000) Long millis) {
    }
}
