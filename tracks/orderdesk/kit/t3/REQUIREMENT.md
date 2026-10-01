# REQ-210 部分发货

## 背景
目前 `POST /api/orders/{id}/ship` 一次发出整单。仓库希望按到货情况分批发货。

## 模型
- 新增**发货单（shipment）**：`id`、`orderId`、`shippedAt`、`lines[{sku, quantity}]`。一个订单可以有多张发货单；每个 sku 的累计发货量不能超过订单数量。
- 新增订单状态 `PARTIALLY_SHIPPED`：已付款且已发出一部分。全部发完时为 `SHIPPED`，订单的 `shippedAt` 等于完成发货的那张发货单的时间。
- 发货时库存的变化与 v1 相同：每发出 1 件，`reserved` 与 `onHand` 各减 1。

## v2 API
1. `POST /api/v2/orders/{id}/shipments`，请求 `{"lines":[{"sku":"A","quantity":2}]}`（1..20 行，quantity 1..1000，sku 不重复）→ **201**，返回发货单 `{id, orderId, shippedAt, lines}`。
   - 订单不存在 → 404 `ORDER_NOT_FOUND`；
   - 订单不是 `PAID` 或 `PARTIALLY_SHIPPED` → 409 `INVALID_STATE`；
   - sku 不在订单中，或数量超过该 sku 剩余未发数量 → 422 `SHIPMENT_INVALID`，不产生任何变化；
   - 请求体不合法 → 400 `VALIDATION_FAILED`。
2. `GET /api/v2/orders/{id}` → v1 订单的全部字段，`status` 为真实状态（可能是 `PARTIALLY_SHIPPED`），每个 line 增加 `shippedQuantity`，并增加 `shipments` 数组（按创建先后排序，每项含 lines）。不存在 → 404。

## v1 兼容（必须满足）
- v1 接口的路径、请求、响应字段与取值保持不变。对历史订单，v1 `GET /api/orders/{id}` 与 `GET /api/orders?customerId=` 的响应必须和发布前**完全一致**。
- v1 客户端不认识 `PARTIALLY_SHIPPED`：在所有 v1 响应中，部分发货的订单显示为 `PAID`。
- v1 `POST /api/orders/{id}/ship` 继续可用：对 `PAID` 或部分发货的订单，把所有剩余数量作为一张发货单发出，订单变为 `SHIPPED`。
- 部分发货的订单不能取消、不能退款（409 `INVALID_STATE`）；其余退款/取消规则不变。

## 历史数据
- 发布前通过 v1 发货的订单都必须在 v2 中显示为**一张**包含全部明细的发货单，`shippedAt` 与订单的 `shippedAt` 相同，各行 `shippedQuantity` 等于订单数量。注意：已发货的订单之后可能被退款，状态不再是 `SHIPPED`。
- 未发货的订单没有发货单。

## 并发
同一订单的并发发货请求不能导致累计发货量超过订单数量，库存不能出现负数或错账。
