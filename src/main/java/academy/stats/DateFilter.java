package academy.stats;

import academy.model.Log;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

// filters logs by date: --from --to
public class DateFilter {

    private final LocalDateTime start;
    private final LocalDateTime end;

    public DateFilter(LocalDate from, LocalDate to) {
        this.start = (from != null) ? from.atStartOfDay() : null;
        this.end = (to != null) ? to.atTime(LocalTime.MAX) : null; // End of day
    }

    public boolean isWithinRange(Log log) {
        LocalDateTime ts = log.timestamp();
        if (start != null && ts.isBefore(start)) {
            return false;
        }
        if (end != null && ts.isAfter(end)) {
            return false;
        }
        return true;
    }
}
