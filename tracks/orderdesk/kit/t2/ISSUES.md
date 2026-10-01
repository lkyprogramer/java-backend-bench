# 待处理工单

## OD-101 下单失败却占用了库存
一个订单包含多种商品，其中一种库存不足时接口正确返回 409 `INSUFFICIENT_STOCK`，但其它商品的 `reserved` 却增加了，订单列表里也没有这个订单。运营反馈这些“幽灵占用”只能人工修数据。

## OD-102 支付被拒，订单却显示已支付
用尾号 0002 的卡支付，接口返回 402 `PAYMENT_DECLINED`；随后查询订单状态却是 `PAID`，`paidAmount` 也等于订单金额。客户换卡重试时报 409。

## OD-103 全额退款后状态仍是“部分退款”
订单付款 40.00，客户申请退款 40，返回的状态是 `PARTIALLY_REFUNDED`，退款金额已经等于付款金额。分两次退完也一样。

## OD-104 日报把凌晨的订单算到了前一天
北京时间凌晨 0–8 点下的订单出现在前一天的日报里。服务器运行在 UTC。

## OD-105 并发入库后库存数变少
仓库系统并发调用 `/api/inventory/{sku}/restock`（每次 1 件，共 32 次），之后 `onHand` 只增加了个位数。大促期间同一商品还出现过超卖。

## OD-106 查询订单详情返回 500
`GET /api/orders/{id}` 对任何订单都返回 500，日志里是 `LazyInitializationException: failed to lazily initialize a collection of role: com.orderdesk.domain.Order.lines`。订单列表接口正常。
