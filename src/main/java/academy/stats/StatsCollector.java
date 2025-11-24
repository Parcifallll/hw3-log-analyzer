package academy.stats;

import academy.model.Log;
import academy.model.Stats;
import academy.model.ResponseSize;
import academy.model.TopResource;
import academy.model.CodeCount;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// collects statistics from logs
public class StatsCollector {

    private final List<Long> sizes = new ArrayList<>();
    private final Map<String, Integer> resources = new HashMap<>();
    private final Map<Integer, Integer> codes = new HashMap<>();
    private long sumSizes = 0;

    public void collect(Log log) {
        long size = log.bodyBytesSent();
        sizes.add(size);
        sumSizes += size;

        resources.merge(log.resource(), 1, Integer::sum);
        codes.merge(log.status(), 1, Integer::sum);
    }

    public Stats getStats(List<String> files, LocalDate from, LocalDate to) {
        int total = sizes.size();
        double avg = total > 0 ? Math.round((double) sumSizes / total * 100.0) / 100.0 : 0;
        double max = total > 0 ? Math.round(sizes.stream().max(Long::compareTo).orElse(0L) * 100.0) / 100.0 : 0;
        double p95 = total > 0 ? Math.round(calculateP95(sizes) * 100.0) / 100.0 : 0;

        // top 10 resources DESC by count
        List<TopResource> topResources = resources.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(10)
            .map(e -> new TopResource(e.getKey(), e.getValue()))
            .collect(Collectors.toList());

        // all response codes ASC by code
        List<CodeCount> responseCodes = codes.entrySet().stream()
            .sorted(Map.Entry.<Integer, Integer>comparingByValue().reversed())
            .map(e -> new CodeCount(e.getKey(), e.getValue()))
            .collect(Collectors.toList());

        return new Stats(files, total, new ResponseSize(avg, max, p95), topResources, responseCodes, from, to);
    }

    private double calculateP95(List<Long> sizes) {
        if (sizes.isEmpty()) {
            return 0;
        }

        List<Long> sorted = new ArrayList<>(sizes);
        sorted.sort(Long::compareTo);

        double pos = 95 / 100.0 * (sorted.size() - 1);
        int idx = (int) pos;
        double frac = pos - idx;

        if (idx >= sorted.size() - 1) {
            return sorted.getLast();
        } else {
            long lower = sorted.get(idx);
            long upper = sorted.get(idx + 1);
            return lower + (upper - lower) * frac;
        }
    }
}
