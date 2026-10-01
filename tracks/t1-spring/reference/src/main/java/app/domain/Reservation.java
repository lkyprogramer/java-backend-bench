package app.domain;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderBy;
import jakarta.persistence.SequenceGenerator;

@Entity
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "reservation_seq")
    @SequenceGenerator(name = "reservation_seq", sequenceName = "reservation_seq", allocationSize = 1)
    private Long id;

    @Column(name = "request_id")
    private String requestId;

    @Enumerated(EnumType.STRING)
    private State state;

    @Column(name = "created_at")
    private long createdAt;

    @Column(name = "expires_at")
    private long expiresAt;

    @Column(name = "ttl_millis")
    private long ttlMillis;

    @ElementCollection
    @CollectionTable(name = "reservation_line", joinColumns = @JoinColumn(name = "reservation_id"))
    @OrderBy("sku")
    private List<Line> lines = new ArrayList<>();

    protected Reservation() {
    }

    public Reservation(String requestId, List<Line> sortedLines, long ttlMillis, long now) {
        this.requestId = requestId;
        this.state = State.ACTIVE;
        this.createdAt = now;
        this.expiresAt = now + ttlMillis;
        this.ttlMillis = ttlMillis;
        this.lines = new ArrayList<>(sortedLines);
    }

    public Long getId() {
        return id;
    }

    public String getRequestId() {
        return requestId;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public long getTtlMillis() {
        return ttlMillis;
    }

    public List<Line> getLines() {
        return lines;
    }

    /** Idempotency payload: sorted (sku, quantity) set plus TTL. */
    public boolean samePayload(List<Line> sortedLines, long ttl) {
        // Hibernate's PersistentBag uses identity equals; compare element-wise.
        return ttlMillis == ttl && List.copyOf(lines).equals(sortedLines);
    }
}
