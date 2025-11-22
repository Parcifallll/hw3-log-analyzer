package academy.parser;

import academy.model.Log;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NginxLogParser implements LogParser {

    private static final Logger logger = LogManager.getLogger(NginxLogParser.class);

    // Regex to match the log format
    private static final Pattern LOG_PATTERN = Pattern.compile(
        "^(\\S+) - (\\S+) \\[(.+?)\\] \"(.+?)\" (\\d+) (\\d+) \"(.+?)\" \"(.+?)\"$"
    );


    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(
        "dd/MMM/yyyy:HH:mm:ss Z", Locale.ENGLISH
    );

    @Override
    public Log parseLine(String line) {
        if (line == null || line.isEmpty()) {
            logger.warn("Empty log line skipped");
            return null;
        }

        Matcher matcher = LOG_PATTERN.matcher(line);
        if (!matcher.matches()) {
            logger.warn("Invalid log format: {}", line);
            return null;
        }

        try {
            // Extract groups
            String timeLocalStr = matcher.group(3);
            String request = matcher.group(4);
            int status = Integer.parseInt(matcher.group(5));
            long bodyBytesSent = Long.parseLong(matcher.group(6));

            LocalDateTime timestamp = LocalDateTime.parse(timeLocalStr, DATE_FORMATTER);

            // Parse resource from request: "GET /path HTTP/1.1" -> "/path"
            String resource = extractResource(request);
            if (resource == null) {
                logger.warn("Invalid request format in line: {}", line);
                return null;
            }

            return new Log(timestamp, resource, status, bodyBytesSent);
        } catch (NumberFormatException | DateTimeParseException e) {
            logger.warn("Parsing error in line: {} - {}", line, e.getMessage());
            return null;
        }
    }

    private String extractResource(String request) {
        String[] parts = request.split(" ");
        if (parts.length == 3) {
            return parts[1];  // resource is the 2 part
        }
        return null;
    }
}
