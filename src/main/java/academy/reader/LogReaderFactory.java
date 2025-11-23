package academy.reader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

// Factory for creating LogReader instances
public class LogReaderFactory {

    private static final Logger logger = LogManager.getLogger(LogReaderFactory.class);

    public static List<LogReader> createReaders(String pathStr) {
        if (pathStr.startsWith("http://") || pathStr.startsWith("https://")) {
            // Remote
            return List.of(new RemoteLogReader(pathStr));
        } else {
            // Local: single or glob
            Path path = Path.of(pathStr);
            if (Files.exists(path) && Files.isRegularFile(path)) {
                // Single local file
                return List.of(new LocalLogReader(path));
            } else {
                // Glob
                try {
                    PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pathStr);
                    List<LogReader> readers = new ArrayList<>();
                    try (Stream<Path> walk = Files.walk(Path.of("."))) {
                        walk.filter(Files::isRegularFile)
                            .filter(matcher::matches)
                            .forEach(p -> readers.add(new LocalLogReader(p)));
                    }
                    if (readers.isEmpty()) {
                        throw new RuntimeException("No files matched glob: " + pathStr);
                    }
                    return readers;
                } catch (IOException e) {
                    logger.error("Error processing glob: {}", pathStr, e);
                    throw new RuntimeException("Failed to process glob: " + pathStr, e);
                }
            }
        }
    }
}
