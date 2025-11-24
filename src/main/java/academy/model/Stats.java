package academy.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public record Stats(
        List<String> files, // paths
        int totalRequestsCount,
        ResponseSize responseSizeInBytes,
        List<TopResource> resources, // top 10 DESC by count
        List<CodeCount> responseCodes, // all codes, sorted by code ASC
        @JsonInclude(JsonInclude.Include.NON_NULL) LocalDate from, // skip if null
        @JsonInclude(JsonInclude.Include.NON_NULL) LocalDate to,
        Set<String> uniqueProtocols // skip if null
        ) {}
