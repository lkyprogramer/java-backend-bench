# OrderDesk

Order and fulfilment service: catalog, stock, orders, card payments, shipping, refunds and a daily sales report.
Spring Boot 3.5 / Java 21 / Spring Data JPA / Flyway / H2.

```bash
mvn -o test                      # existing test suite (in-memory database)
mvn -o compile dependency:build-classpath -Dmdep.outputFile=target/cp.txt
java -cp target/classes:$(cat target/cp.txt) com.orderdesk.OrderDeskApplication   # data in ./data
```

## API (v1)

| Method | Path | Notes |
|---|---|---|
| POST | `/api/products` | `{sku, name, price, initialStock}` → 201 product with stock |
| GET | `/api/products/{sku}` | product with `onHand`, `reserved`, `available` |
| POST | `/api/inventory/{sku}/restock` | `{quantity}` adds to on-hand stock |
| POST | `/api/orders` | `{customerId, lines:[{sku, quantity}]}` → 201; reserves stock; orders that cost nothing are settled at once |
| GET | `/api/orders/{id}` | order with lines |
| GET | `/api/orders?customerId=` | order summaries of a customer |
| POST | `/api/orders/{id}/pay` | `{cardNumber}`; 402 `PAYMENT_DECLINED` when the card is refused |
| POST | `/api/orders/{id}/ship` | paid orders only; reserved stock leaves the warehouse |
| POST | `/api/orders/{id}/cancel` | unpaid orders only; releases the reservation |
| POST | `/api/orders/{id}/refunds` | `{amount}`; `PARTIALLY_REFUNDED` until the whole payment is refunded, then `REFUNDED` |
| GET | `/api/reports/daily?date=YYYY-MM-DD` | orders placed during that business day (excluding cancelled) |

Errors are `{"error": {"code": "...", "message": "..."}}`.

## Conventions

- **Time** always comes from the `java.time.Clock` bean (`TimeConfig`); never call `Instant.now()` / `LocalDate.now()` directly.
- **Business day** is defined in `orderdesk.business-zone` (default `Asia/Shanghai`); servers run in UTC.
- **Money** is `BigDecimal` with two decimals in the database. Compare amounts by value.
- **Transactions** live in the service layer. Entities never leave it: `spring.jpa.open-in-view=false` stays off, and
  lazy-loading workarounds (`enable_lazy_load_no_trans`, blanket `EAGER`) are not accepted in review.
- **Schema** changes go into a new Flyway migration (`V3__...`); released migrations are never edited.
- **Card processor**: `SimulatedPaymentGateway` declines cards ending in `0002`; any other 16-digit card is captured.
