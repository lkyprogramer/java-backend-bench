package app.service;

/** A contract error: HTTP status plus frozen error code. */
public class ApiException extends RuntimeException {

    private final int status;
    private final String code;

    public ApiException(int status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public static ApiException invalid(String message) {
        return new ApiException(400, "INVALID_REQUEST", message);
    }

    public static ApiException notFound(String message) {
        return new ApiException(404, "NOT_FOUND", message);
    }

    public static ApiException conflict(String code, String message) {
        return new ApiException(409, code, message);
    }

    public int status() {
        return status;
    }

    public String code() {
        return code;
    }
}
