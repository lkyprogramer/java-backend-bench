package com.orderdesk.web;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.orderdesk.service.ReportService;
import com.orderdesk.web.dto.DailyReport;

@RestController
public class ReportController {

    private final ReportService reports;

    public ReportController(ReportService reports) {
        this.reports = reports;
    }

    @GetMapping("/api/reports/daily")
    public DailyReport daily(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return reports.daily(date);
    }
}
