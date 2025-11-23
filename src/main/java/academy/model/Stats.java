package academy.model;

import java.time.LocalDate;
import java.util.List;

public record Stats(
    List<String> files, // paths
    int totalRequestsCount,
    ResponseSize responseSizeInBytes,
    List<TopResource> resources, // top 10 DESC by count
    List<CodeCount> responseCodes, // all codes, sorted by code ASC
    LocalDate from, // may be null
    LocalDate to // may be null
) {
}
