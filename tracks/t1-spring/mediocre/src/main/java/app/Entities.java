package app;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;

public class Entities {

    @Entity
    @Table(name = "items")
    public static class ItemEntity {
        @Id
        public String sku;
        public Long total;
        public Long available;
        public Long held;
        public Long sold;
    }

    @Entity
    @Table(name = "reservations")
    public static class ReservationEntity {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        public Long id;
        @Column(name = "request_id")
        public String requestId;
        public String state;
        @Column(name = "created_at")
        public Long createdAt;
        @Column(name = "expires_at")
        public Long expiresAt;
        @Column(name = "ttl_millis")
        public Long ttlMillis;
        @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
        @JoinColumn(name = "reservation_id")
        @OrderBy("sku")
        public List<LineEntity> lines = new ArrayList<>();
    }

    @Entity
    @Table(name = "reservation_lines")
    public static class LineEntity {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        public Long id;
        public String sku;
        public Long quantity;
    }

    @Entity
    @Table(name = "settings")
    public static class Setting {
        @Id
        public String name;
        public Long val;
    }
}
