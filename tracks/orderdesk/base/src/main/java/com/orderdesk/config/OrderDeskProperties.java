package com.orderdesk.config;

import java.math.BigDecimal;
import java.time.ZoneId;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param businessZone  time zone in which the business day (reports, cut-offs) is defined
 * @param maxOrderAmount upper bound for a single order total
 */
@ConfigurationProperties(prefix = "orderdesk")
public record OrderDeskProperties(ZoneId businessZone, BigDecimal maxOrderAmount) {
}
