package app.web;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import app.service.ApiException;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;

/** Every error, including container-level ones routed to /error, uses the frozen JSON envelope. */
public final class Errors {

    private static final Logger LOG = LoggerFactory.getLogger(Errors.class);

    private Errors() {
    }

    static ResponseEntity<Map<String, Object>> body(int status, String code, String message) {
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("error", Map.of("code", code, "message", message)));
    }

    @RestControllerAdvice
    public static class Advice {

        @ExceptionHandler(ApiException.class)
        ResponseEntity<Map<String, Object>> api(ApiException e) {
            return body(e.status(), e.code(), e.getMessage());
        }

        @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
        ResponseEntity<Map<String, Object>> unknown(Exception e) {
            return body(404, "NOT_FOUND", "Unknown route");
        }

        @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
        ResponseEntity<Map<String, Object>> method(Exception e) {
            return body(405, "METHOD_NOT_ALLOWED", "Method not allowed");
        }

        @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
        ResponseEntity<Map<String, Object>> media(Exception e) {
            return body(415, "UNSUPPORTED_MEDIA_TYPE", "Content-Type must be application/json");
        }

        @ExceptionHandler(Exception.class)
        ResponseEntity<Map<String, Object>> unexpected(Exception e) {
            LOG.error("Unexpected error", e);
            return body(500, "INTERNAL_ERROR", "Unexpected server error");
        }
    }

    @RestController
    public static class Fallback implements ErrorController {

        @RequestMapping("/error")
        ResponseEntity<Map<String, Object>> error(HttpServletRequest request) {
            Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
            int code = status instanceof Integer i ? i : 500;
            return switch (code) {
                case 400 -> body(400, "INVALID_REQUEST", "Malformed request");
                case 404 -> body(404, "NOT_FOUND", "Unknown route");
                case 405 -> body(405, "METHOD_NOT_ALLOWED", "Method not allowed");
                case 413 -> body(413, "PAYLOAD_TOO_LARGE", "Request body exceeds 65536 bytes");
                case 415 -> body(415, "UNSUPPORTED_MEDIA_TYPE", "Content-Type must be application/json");
                default -> body(500, "INTERNAL_ERROR", "Unexpected server error");
            };
        }
    }
}
