package academy.acceptance;

import academy.model.Stats;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class StatsReportTest {

    private static final String JAR_PATH = "target/hw3-logs-1.0.jar";
    private Path tempLog;
    private Path tempOutput;

    @BeforeEach
    void setUp() throws IOException {
        tempLog = Files.createTempFile("test", ".log");
        Files.writeString(tempLog, """
            93.180.71.3 - - [17/May/2015:08:05:32 +0000] "GET /downloads/product_1 HTTP/1.1" 304 0 "-" "Debian APT-HTTP/1.3 (0.8.16~exp12ubuntu10.21)"
            93.180.71.3 - - [17/May/2015:08:05:23 +0000] "GET /downloads/product_1 HTTP/1.1" 304 0 "-" "Debian APT-HTTP/1.3 (0.8.16~exp12ubuntu10.21)"
            80.91.33.133 - - [17/May/2015:08:05:24 +0000] "GET /downloads/product_1 HTTP/1.1" 304 0 "-" "Debian APT-HTTP/1.3 (0.8.16~exp12ubuntu10.17)"
            """);
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(tempLog);
        Files.deleteIfExists(tempOutput);
    }

    @ParameterizedTest
    @ValueSource(strings = {"json", "markdown"})
    @DisplayName("Сохранение статистики в формате {0}")
    void reportTest(String format) throws IOException, InterruptedException {
        String ext = "json".equals(format) ? ".json" : ".md";
        tempOutput = Path.of("rep" + ext);
        Process process = new ProcessBuilder("java", "-jar", JAR_PATH, "--path", tempLog.toString(), "--format", format, "--output", tempOutput.toString()).start();
        process.waitFor();
        assertEquals(0, process.exitValue());

        if ("json".equals(format)) {
            ObjectMapper mapper = new ObjectMapper();
            Stats stats = mapper.readValue(tempOutput.toFile(), Stats.class);
            assertEquals(3, stats.totalRequestsCount());
            assertEquals(1, stats.resources().size());
        } else {
            String md = Files.readString(tempOutput);
            System.out.println(md);
            assertTrue(md.contains("Количество запросов  | 3"));
            assertTrue(md.contains("Not Modified | 3"));

        }
    }

    private String getError(Process process) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        return reader.lines().collect(Collectors.joining("\n"));
    }

//    @Test
//    @DisplayName("Сохранение статистики в формате ADOC")
//    void adocTest() {
//        fail("Not implemented yet");  // Optional, keep fail
//    }
}
