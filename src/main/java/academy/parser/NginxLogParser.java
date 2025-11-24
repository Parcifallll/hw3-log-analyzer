package academy.parser;

import academy.model.Log;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class NginxLogParser implements LogParser {

    private static final Logger logger = LogManager.getLogger(NginxLogParser.class);

    // match the log format
    private static final Pattern LOG_PATTERN =
            Pattern.compile("^(\\S+) - (\\S+) \\[(.+?)\\] \"(.+?)\" (\\d+) (\\d+) \"(.+?)\" \"(.+?)\"$");

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss Z", Locale.ENGLISH);

    @Override
    public Log parseLine(String line, String fileName, int lineNum) {
        if (line == null || line.isEmpty()) {
            logger.warn("<{}: {} line>: Empty log line skipped", fileName, lineNum);
            return null;
        }

        Matcher matcher = LOG_PATTERN.matcher(line);
        if (!matcher.matches()) {
            logger.warn("<{}: {} line>: Invalid log format: \"{}\"", fileName, lineNum, line);
            return null;
        }

        try {
            String timeLocalStr = matcher.group(3);
            String request = matcher.group(4);
            int status = Integer.parseInt(matcher.group(5));
            long bodyBytesSent = Long.parseLong(matcher.group(6));

            // Parse timestamp
            LocalDateTime timestamp = LocalDateTime.parse(timeLocalStr, DATE_FORMATTER);

            // Parse request: method, resource, protocol
            String[] requestParts = request.split("\\s+");
            String resource;
            String protocol = "UNKNOWN";

            if (requestParts.length >= 2) {
                resource = requestParts[1];
                if (requestParts.length >= 3) {
                    protocol = requestParts[2]; // HTTP/1.1, HTTP/2.0, ...
                }
            } else {
                logger.warn("<{}: {} line>: Invalid request format in line: {}", fileName, lineNum, line);
                return null;
            }

            return new Log(timestamp, resource, status, bodyBytesSent, protocol);

        } catch (NumberFormatException | DateTimeParseException e) {
            logger.warn("<{}: {} line>: Parsing error in line: {} - {}", fileName, lineNum, line, e.getMessage());
            return null;
        }
    }
}
