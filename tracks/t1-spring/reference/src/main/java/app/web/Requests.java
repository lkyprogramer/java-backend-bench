package app.web;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import app.service.ApiException;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Request parsing in contract order: media type (415), size (413), UTF-8 and JSON syntax (400),
 * then exact field sets and integer lexical forms. The application ObjectMapper is configured for
 * strict duplicate-key and trailing-token detection; values are inspected as a tree so that
 * strings, booleans, decimals and exponents are never coerced into integers.
 */
@Component
public class Requests {

    static final int MAX_BODY = 65536;
    private static final Pattern SKU = Pattern.compile("[A-Za-z0-9_-]{1,32}");
    private static final Pattern REQUEST_ID = Pattern.compile("[A-Za-z0-9_-]{1,64}");

    private final ObjectMapper mapper;

    public Requests(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public JsonNode object(HttpServletRequest request) throws IOException {
        if (!isJson(request.getContentType())) {
            throw new ApiException(415, "UNSUPPORTED_MEDIA_TYPE", "Content-Type must be application/json");
        }
        byte[] bytes = request.getInputStream().readNBytes(MAX_BODY + 1);
        if (bytes.length > MAX_BODY) {
            throw new ApiException(413, "PAYLOAD_TOO_LARGE", "Request body exceeds 65536 bytes");
        }
        try {
            StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes));
            JsonNode node = mapper.readTree(bytes);
            if (node == null || !node.isObject()) {
                throw ApiException.invalid("Request body must be a JSON object");
            }
            return node;
        } catch (CharacterCodingException | JsonProcessingException e) {
            throw ApiException.invalid("Request body is not valid UTF-8 JSON");
        }
    }

    private static boolean isJson(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return false;
        }
        try {
            return MediaType.APPLICATION_JSON.equalsTypeAndSubtype(MediaType.parseMediaType(contentType));
        } catch (InvalidMediaTypeException e) {
            return false;
        }
    }

    public static void fields(JsonNode node, String... names) {
        Set<String> actual = new TreeSet<>();
        for (Iterator<String> it = node.fieldNames(); it.hasNext();) {
            actual.add(it.next());
        }
        if (!actual.equals(new TreeSet<>(Set.of(names)))) {
            throw ApiException.invalid("Fields must be exactly " + new TreeSet<>(Set.of(names)));
        }
    }

    public static long integer(JsonNode node, long min, long max) {
        if (node == null || !node.isIntegralNumber() || !node.canConvertToLong()) {
            throw ApiException.invalid("Expected an integer");
        }
        long value = node.longValue();
        if (value < min || value > max) {
            throw ApiException.invalid("Integer out of range");
        }
        return value;
    }

    public static String sku(JsonNode node) {
        return text(node, SKU);
    }

    public static String requestId(JsonNode node) {
        return text(node, REQUEST_ID);
    }

    public static String sku(String raw) {
        if (raw == null || !SKU.matcher(raw).matches()) {
            throw ApiException.invalid("Invalid sku");
        }
        return raw;
    }

    private static String text(JsonNode node, Pattern pattern) {
        if (node == null || !node.isTextual() || !pattern.matcher(node.textValue()).matches()) {
            throw ApiException.invalid("Invalid identifier");
        }
        return node.textValue();
    }

    /** Positive (or, with zeroAllowed, non-negative) decimal integer without sign; 64-bit. */
    public static long decimal(String raw, boolean zeroAllowed) {
        if (raw == null || raw.isEmpty() || !raw.chars().allMatch(c -> c >= '0' && c <= '9')) {
            throw ApiException.invalid("Expected a decimal integer");
        }
        try {
            long value = Long.parseLong(raw);
            if (value < (zeroAllowed ? 0 : 1)) {
                throw ApiException.invalid("Integer out of range");
            }
            return value;
        } catch (NumberFormatException e) {
            throw ApiException.invalid("Integer out of range");
        }
    }

    public static void noQuery(HttpServletRequest request) {
        if (request.getQueryString() != null) {
            throw ApiException.invalid("This route accepts no query parameters");
        }
    }

    /** Raw query parsing so that duplicate, empty and unknown keys are all visible. */
    public static Map<String, String> query(HttpServletRequest request, Set<String> allowed) {
        Map<String, String> out = new HashMap<>();
        String raw = request.getQueryString();
        if (raw == null || raw.isEmpty()) {
            return out;
        }
        for (String part : raw.split("&", -1)) {
            String[] pair = part.split("=", 2);
            if (pair.length != 2 || !allowed.contains(pair[0]) || out.putIfAbsent(pair[0], pair[1]) != null) {
                throw ApiException.invalid("Invalid query parameter");
            }
        }
        return out;
    }
}
