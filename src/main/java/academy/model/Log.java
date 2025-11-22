package academy.model;

import java.time.LocalDateTime;

public record Log(
    LocalDateTime timestamp,
    String resource, // /path
    int status, // HTTP code
    long bodySent // response size (bytes)
) {
}
