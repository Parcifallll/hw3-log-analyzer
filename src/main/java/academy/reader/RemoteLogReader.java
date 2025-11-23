package academy.reader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.stream.Stream;

public class RemoteLogReader implements LogReader {

    private static final Logger logger = LogManager.getLogger(RemoteLogReader.class);

    private final String url;

    public RemoteLogReader(String url) {
        this.url = url;
    }

    @Override
    public Stream<String> readLines() {
        try {
            URI uri = new URI(url);
            HttpRequest request = HttpRequest.newBuilder(uri).GET().build();
            HttpResponse<InputStream> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 200) {
                throw new IOException("Failed to fetch remote file, status: " + response.statusCode());
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(response.body()));
            return reader.lines().onClose(() -> {
                try {
                    reader.close();
                } catch (IOException e) {
                    logger.warn("Error closing remote stream", e);
                }
            });
        } catch (URISyntaxException | IOException | InterruptedException e) {
            logger.error("Error reading remote file: {}", url, e);
            throw new RuntimeException("Failed to read remote file: " + url, e);
        }
    }
}
