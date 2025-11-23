package academy.model;

import java.util.List;

public record Stats(
    List<String> files, // processed files paths
    int totalRequestsCount,
    ResponseSize responseSizeInBytes,
    List<TopResource> resources, // top 10 DESC by count
    List<CodeCount> responseCodes // all codes, sorted by code ASC
) {
}
