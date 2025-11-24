package academy.reader;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// Factory for creating LogReader instances
public class LogReaderFactory {

    private static final Logger logger = LogManager.getLogger(LogReaderFactory.class);

    public static List<LogReader> createReaders(String pathStr) {
        if (pathStr.startsWith("http://") || pathStr.startsWith("https://")) {
            // Remote
            return List.of(new RemoteLogReader(pathStr));
        } else {
            boolean isGlob = pathStr.contains("*")
                    || pathStr.contains("?")
                    || pathStr.contains("[")
                    || pathStr.contains("{")
                    || pathStr.contains("}");

            if (isGlob) {
                // Glob pattern
                // Parse root manually (before wildcard)
                int lastSlash = pathStr.lastIndexOf('/');
                Path root;
                String globPattern = pathStr;
                if (lastSlash > 0) {
                    String rootStr = pathStr.substring(0, lastSlash);
                    try {
                        root = Path.of(rootStr);
                    } catch (InvalidPathException e) {
                        throw new RuntimeException("Invalid glob root: " + rootStr, e);
                    }
                    globPattern = pathStr.substring(lastSlash + 1); // Only the glob part
                } else {
                    root = Path.of(".");
                }

                try {
                    PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + globPattern);
                    List<LogReader> readers = new ArrayList<>();
                    try (Stream<Path> walk = Files.walk(root)) {
                        walk.filter(Files::isRegularFile)
                                .filter(p -> matcher.matches(p.getFileName())) // Match file name only
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
            } else {
                // Single local file
                Path path;
                try {
                    path = Path.of(pathStr);
                } catch (InvalidPathException e) {
                    throw new RuntimeException("Invalid path: " + pathStr, e);
                }
                if (!Files.exists(path) || !Files.isRegularFile(path)) {
                    throw new RuntimeException("File not found: " + pathStr);
                }
                return List.of(new LocalLogReader(path));
            }
        }
    }
}
