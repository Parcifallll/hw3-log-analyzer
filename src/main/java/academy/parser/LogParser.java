package academy.parser;

import academy.model.Log;

public interface LogParser {
    /**
     * Parses a single log line.
     * @param line the log line to parse
     * @return LogEntry if successful, null if invalid (with WARN log)
     */
    Log parseLine(String line);
}
