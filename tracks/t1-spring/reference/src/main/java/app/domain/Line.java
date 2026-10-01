package app.domain;

import jakarta.persistence.Embeddable;

@Embeddable
public record Line(String sku, long quantity) {
}
