package com.orderdesk.web;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.orderdesk.payment.PaymentDeclinedException;
import com.orderdesk.service.BusinessException;

/** Error body: {"error": {"code": ..., "message": ...}}. */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<Map<String, Object>> business(BusinessException e) {
        return error(e.getStatus(), e.getCode(), e.getMessage());
    }

    @ExceptionHandler(PaymentDeclinedException.class)
    ResponseEntity<Map<String, Object>> declined(PaymentDeclinedException e) {
        return error(HttpStatus.PAYMENT_REQUIRED, "PAYMENT_DECLINED", e.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<Map<String, Object>> invalid(Exception e) {
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Invalid request");
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, Object>> unexpected(Exception e) {
        LOG.error("Unhandled error", e);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Unexpected error");
    }

    private static ResponseEntity<Map<String, Object>> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(Map.of("error", Map.of("code", code, "message", message)));
    }
}
