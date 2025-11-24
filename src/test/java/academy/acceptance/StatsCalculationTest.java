package academy.acceptance;

import static org.junit.jupiter.api.Assertions.assertEquals;

import academy.model.Stats;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class StatsCalculationTest {

    private static final String JAR_PATH = "target/hw3-logs-1.0.jar";
    private Path tempLog;
    private Path tempOutput;

    @BeforeEach
    void setUp() throws IOException {
        tempLog = Files.createTempFile("test", ".log");
        Files.writeString(
                tempLog,
                """
            93.180.71.3 - - [17/May/2015:08:05:32 +0000] "GET /downloads/product_1 HTTP/1.1" 304 0 "-" "Debian APT-HTTP/1.3 (0.8.16~exp12ubuntu10.21)"
            93.180.71.3 - - [17/May/2015:08:05:23 +0000] "GET /downloads/product_1 HTTP/1.1" 304 0 "-" "Debian APT-HTTP/1.3 (0.8.16~exp12ubuntu10.21)"
            80.91.33.133 - - [17/May/2015:08:05:24 +0000] "GET /downloads/product_1 HTTP/1.1" 304 0 "-" "Debian APT-HTTP/1.3 (0.8.16~exp12ubuntu10.17)"
            """);
        tempOutput = Path.of("rep.json");
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(tempLog);
        Files.deleteIfExists(tempOutput);
    }

    @Test
    @DisplayName("Расчёт статистики на основании локального log-файла")
    void happyPathTest() throws IOException, InterruptedException {
        Process process = new ProcessBuilder(
                        "java",
                        "-jar",
                        JAR_PATH,
                        "--path",
                        tempLog.toString(),
                        "--format",
                        "json",
                        "--output",
                        tempOutput.toString())
                .start();
        process.waitFor();
        assertEquals(0, process.exitValue());

        ObjectMapper mapper = new ObjectMapper();
        Stats stats = mapper.readValue(tempOutput.toFile(), Stats.class);
        assertEquals(3, stats.totalRequestsCount());
        assertEquals(0.0, stats.responseSizeInBytes().average());
        assertEquals(1, stats.resources().size());
        assertEquals(1, stats.responseCodes().size()); // 304
    }
}
