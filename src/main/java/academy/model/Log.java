package academy.model;

import java.time.LocalDateTime;

// Immutable model for an NGINX log

public record Log(
    LocalDateTime timestamp,
    String resource, // /path
    int status, // HTTP code
    long bodyBytesSent // response size (bytes)
) {
}
