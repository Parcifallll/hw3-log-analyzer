package academy.model;

import java.util.List;

public record Stats(
    List<String> files, // processed files paths
    int totalRequestsCount,
    ResponseSize responseSizeInBytes,
    List<TopResource> resources, // DESC by count
    List<CodeCount> responseCodes // DESC by count
) {
}
