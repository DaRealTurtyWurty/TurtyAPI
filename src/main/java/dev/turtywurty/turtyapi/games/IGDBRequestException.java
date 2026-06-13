package dev.turtywurty.turtyapi.games;

import com.api.igdb.exceptions.RequestException;
import org.jetbrains.annotations.Nullable;

public final class IGDBRequestException extends RuntimeException {
    private final String operation;
    private final Integer upstreamStatus;

    private IGDBRequestException(String operation, @Nullable Integer upstreamStatus, Throwable cause) {
        super("IGDB request failed while " + operation, cause);
        this.operation = operation;
        this.upstreamStatus = upstreamStatus;
    }

    public static IGDBRequestException requestFailed(String operation, RequestException cause) {
        return new IGDBRequestException(operation, cause.getStatusCode(), cause);
    }

    public static IGDBRequestException invalidResponse(String operation, RuntimeException cause) {
        return new IGDBRequestException(operation, null, cause);
    }

    public String getOperation() {
        return this.operation;
    }

    public @Nullable Integer getUpstreamStatus() {
        return this.upstreamStatus;
    }

    public int getClientStatus() {
        if (this.upstreamStatus == null || this.upstreamStatus <= 0) {
            return 503;
        }

        return switch (this.upstreamStatus) {
            case 400, 422 -> 400;
            case 404 -> 404;
            case 408, 504 -> 504;
            case 429 -> 503;
            default -> 502;
        };
    }

    public String getErrorCode() {
        if (this.upstreamStatus == null || this.upstreamStatus <= 0) {
            return "igdb_unavailable";
        }

        return switch (this.upstreamStatus) {
            case 400, 422 -> "invalid_igdb_request";
            case 404 -> "igdb_endpoint_not_found";
            case 408, 504 -> "igdb_timeout";
            case 429 -> "igdb_rate_limited";
            case 401, 403 -> "igdb_authentication_failed";
            default -> this.upstreamStatus >= 500
                    ? "igdb_unavailable"
                    : "igdb_request_failed";
        };
    }

    public String getClientMessage() {
        return switch (getErrorCode()) {
            case "invalid_igdb_request" -> "IGDB rejected the request. Check the supplied query and fields.";
            case "igdb_timeout" -> "IGDB did not respond in time.";
            case "igdb_rate_limited" -> "IGDB is temporarily rate limited. Try again later.";
            case "igdb_authentication_failed" -> "The server could not authenticate with IGDB.";
            case "igdb_endpoint_not_found" -> "The requested IGDB endpoint was not found.";
            case "igdb_unavailable" -> "IGDB is temporarily unavailable.";
            default -> "The request to IGDB failed.";
        };
    }
}
