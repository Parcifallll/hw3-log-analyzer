package academy.reader;

import java.util.stream.Stream;

// reading log lines from a source
public interface LogReader {
    Stream<String> readLines();
}
