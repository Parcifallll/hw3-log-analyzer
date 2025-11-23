package academy.reader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

// Reads lines from local file
public class LocalLogReader implements LogReader {

    private static final Logger logger = LogManager.getLogger(LocalLogReader.class);

    private final Path filePath;

    public LocalLogReader(Path filePath) {
        this.filePath = filePath;
    }

    @Override
    public Stream<String> readLines() {
        try {
            return Files.lines(filePath);
        } catch (IOException e) {
            logger.error("Error reading local file: {}", filePath, e);
            throw new RuntimeException("Failed to read local file: " + filePath, e);
        }
    }
}
