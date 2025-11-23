package academy.validator;

import academy.cli.RunCommand;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.time.LocalDate;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// checks paths, format, output, dates
public class ArgumentValidator {

    private static final Logger logger = LogManager.getLogger(ArgumentValidator.class);

    private static final Set<String> FORMATS = Set.of("json", "markdown");
    private static final Set<String> EXTENSIONS = Set.of(".log", ".txt");

    public void validate(RunCommand command) {
        validatePaths(command.getPaths());
        validateFormat(command.getFormat());
        validateOutput(command.getOutput(), command.getFormat());
        validateDates(command.getFrom(), command.getTo());
    }

    private void validatePaths(String[] paths) {
        if (paths == null || paths.length == 0) {
            throw new InvalidArgumentException("No paths provided");
        }

        for (String pathStr : paths) {
            if (isRemoteUrl(pathStr)) {
                validateRemotePath(pathStr);
            } else {
                validateLocalPath(pathStr);
            }
        }
    }

    private boolean isRemoteUrl(String path) {
        return path.startsWith("http://") || path.startsWith("https://");
    }

    private void validateRemotePath(String urlStr) {
        try {
            URI uri = new URI(urlStr);
            HttpRequest request = HttpRequest.newBuilder(uri).method("HEAD", HttpRequest.BodyPublishers.noBody()).build();
            HttpResponse<Void> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.discarding());
            int code = response.statusCode();
            if (code == HttpURLConnection.HTTP_NOT_FOUND) {
                throw new InvalidArgumentException("Remote file not found (404): " + urlStr);
            } else if (code >= 400) {
                throw new InvalidArgumentException("Error accessing remote file (" + code + "): " + urlStr);
            }
            // Check extension
            if (!hasExtension(urlStr)) {
                throw new InvalidArgumentException("Unsupported file format for: " + urlStr);
            }
        } catch (URISyntaxException | IOException | InterruptedException e) {
            throw new InvalidArgumentException("Invalid remote path: " + urlStr, e);
        }
    }

    private void validateLocalPath(String pathStr) {
        Path path;
        try {
            path = Path.of(pathStr);
        } catch (InvalidPathException e) {
            throw new InvalidArgumentException("Invalid Windows path: " + pathStr, e);
        }

        boolean isGlob = pathStr.contains("*") || pathStr.contains("?") || pathStr.contains("[");  // glob pattern

        if (isGlob) {
            try {
                PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pathStr);
                Path root = path.getParent() != null ? path.getParent() : Path.of(".");  // go from parent for absolute globs
                boolean hasMatches = Files.walk(root).anyMatch(p -> matcher.matches(p) && hasExtension(p.toString()));
                if (!hasMatches) {
                    throw new InvalidArgumentException("No matching files found for glob: " + pathStr);
                }
            } catch (IOException e) {
                throw new InvalidArgumentException("Error checking local path: " + pathStr, e);
            }
        } else {
            // Single file
            if (!Files.exists(path) || !Files.isRegularFile(path)) {
                throw new InvalidArgumentException("File not found: " + pathStr);
            }
            if (!hasExtension(pathStr)) {
                throw new InvalidArgumentException("Unsupported file format: " + pathStr);
            }
        }

    }

    private boolean hasExtension(String path) {
        return EXTENSIONS.stream().anyMatch(path::endsWith);
    }

    private void validateFormat(String format) {
        if (!FORMATS.contains(format.toLowerCase())) {
            throw new InvalidArgumentException("Unsupported format: " + format + ". Supported: json, markdown");
        }
    }

    private void validateOutput(Path output, String format) {
        if (Files.exists(output)) {
            throw new InvalidArgumentException("Output file already exists: " + output);
        }

        Path parent = output.getParent();
        if (parent != null && !Files.isWritable(parent)) {
            throw new InvalidArgumentException("Output directory not writable: " + parent);
        }

        String expectedExt = switch (format.toLowerCase()) {
            case "json" -> ".json";
            case "markdown" -> ".md";
            default -> throw new IllegalStateException("Unexpected format");
        };

        if (!output.toString().endsWith(expectedExt)) {
            throw new InvalidArgumentException("Output file extension must be " + expectedExt + " for format " + format);
        }
    }

    private void validateDates(LocalDate from, LocalDate to) {
        if (from != null && to != null && !from.isBefore(to)) {
            throw new InvalidArgumentException("FROM date must be before TO date: from=" + from + ", to=" + to);
        }
    }
}
