package com.orderdesk.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyReport(LocalDate date, String zone, int orderCount, BigDecimal grossAmount) {
}
