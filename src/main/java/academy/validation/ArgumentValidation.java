package academy.validation;

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
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Set;

// checks paths, format, output, dates
public class ArgumentValidation {

    // private static final Logger logger = LogManager.getLogger(ArgumentValidation.class);

    private static final Set<String> FORMATS = Set.of("json", "markdown");
    private static final Set<String> EXTENSIONS = Set.of(".log", ".txt");

    public void validate(RunCommand command) {
        validatePaths(command.getPaths());
        validateFormat(command.getFormat());
        validateDates(command.getFromStr(), command.getToStr(), command);
        validateOutput(command.getOutput(), command.getFormat());
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
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10)) // 10 sec timeout
                    .build();
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
            int code = response.statusCode();
            if (code == HttpURLConnection.HTTP_NOT_FOUND) {
                throw new InvalidArgumentException("Remote file not found (404): " + urlStr);
            } else if (code >= 400) {
                throw new InvalidArgumentException("Error accessing remote file (" + code + "): " + urlStr);
            }
        } catch (URISyntaxException | InterruptedException e) {
            throw new InvalidArgumentException("Invalid remote path: " + urlStr, e);
        } catch (IOException e) {
            throw new InvalidArgumentException("Remote file not found (404): " + urlStr, e); // For no response/timeout
        }
    }

    private void validateLocalPath(String pathStr) {
        boolean isGlob = pathStr.contains("*");

        if (isGlob) {
            // glob pattern
            // parse root manually
            int lastSlash = pathStr.lastIndexOf('/');
            Path root;
            String globPattern = pathStr;
            if (lastSlash > 0) {
                String rootStr = pathStr.substring(0, lastSlash);
                try {
                    root = Path.of(rootStr);
                } catch (InvalidPathException e) {
                    throw new InvalidArgumentException("Invalid glob root: " + rootStr, e);
                }
                globPattern = pathStr.substring(lastSlash + 1); // only the glob part
            } else {
                root = Path.of(".");
            }

            try {
                PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + globPattern);
                boolean hasMatches =
                        Files.walk(root).anyMatch(p -> matcher.matches(p.getFileName()) && hasExtension(p.toString()));
                if (!hasMatches) {
                    throw new InvalidArgumentException("No matching files found for glob: " + pathStr);
                }
            } catch (IOException e) {
                throw new InvalidArgumentException("Error checking glob path: " + pathStr, e);
            }
        } else {
            // Single file
            Path path;
            try {
                path = Path.of(pathStr);
            } catch (InvalidPathException e) {
                throw new InvalidArgumentException("Invalid path: " + pathStr, e);
            }
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

        String expectedExt =
                switch (format.toLowerCase()) {
                    case "json" -> ".json";
                    case "markdown" -> ".md";
                    default -> throw new IllegalStateException("Unexpected format");
                };

        if (!output.toString().endsWith(expectedExt)) {
            throw new InvalidArgumentException(
                    "Output file extension must be " + expectedExt + " for format " + format);
        }
    }

    // only ISO8601
    private void validateDates(String fromStr, String toStr, RunCommand command) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE; // Strict yyyy-MM-dd

        LocalDate from = null;
        if (fromStr != null) {
            try {
                from = LocalDate.parse(fromStr, formatter); // Parse and check format
            } catch (DateTimeParseException e) {
                throw new InvalidArgumentException("Invalid format for --from: must be yyyy-MM-dd");
            }
        }

        LocalDate to = null;
        if (toStr != null) {
            try {
                to = LocalDate.parse(toStr, formatter); // Parse and check format
            } catch (DateTimeParseException e) {
                throw new InvalidArgumentException("Invalid format for --to: must be yyyy-MM-dd");
            }
        }

        if (from != null && to != null && !from.isBefore(to)) {
            throw new InvalidArgumentException("FROM date must be before TO date");
        }

        // Set parsed to command
        command.setFrom(from);
        command.setTo(to);
    }
}
