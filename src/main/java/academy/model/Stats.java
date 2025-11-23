package academy.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;
import java.util.List;

public record Stats(
    List<String> files, // paths
    int totalRequestsCount,
    ResponseSize responseSizeInBytes,
    List<TopResource> resources, // top 10 DESC by count
    List<CodeCount> responseCodes, // all codes, sorted by code ASC
    @JsonInclude(JsonInclude.Include.NON_NULL) LocalDate from, // skip if null
    @JsonInclude(JsonInclude.Include.NON_NULL) LocalDate to // skip if null
) {
}
