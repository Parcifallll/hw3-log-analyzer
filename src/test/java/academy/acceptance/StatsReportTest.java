package academy.acceptance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import academy.cli.RunCommand;
import academy.model.Stats;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

public class StatsReportTest {

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
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(tempLog);
        Files.deleteIfExists(tempOutput);
    }

    @Test
    @DisplayName("Сохранение статистики в формате JSON")
    void jsonTest() throws IOException {
        tempOutput = Path.of("output.json");
        String[] args = {
            "--path", tempLog.toString(),
            "--format", "json",
            "--output", tempOutput.toString()
        };

        int exitCode = new CommandLine(new RunCommand()).execute(args);
        assertEquals(0, exitCode);

        ObjectMapper mapper = new ObjectMapper();
        Stats stats = mapper.readValue(tempOutput.toFile(), Stats.class);
        assertEquals(3, stats.totalRequestsCount());
        assertEquals(1, stats.resources().size());
    }

    @Test
    @DisplayName("Сохранение статистики в формате MARKDOWN")
    void markdownTest() throws IOException {
        tempOutput = Path.of("output.md");
        String[] args = {
            "--path", tempLog.toString(),
            "--format", "markdown",
            "--output", tempOutput.toString()
        };

        int exitCode = new CommandLine(new RunCommand()).execute(args);
        assertEquals(0, exitCode);

        String md = Files.readString(tempOutput);
        assertTrue(md.contains("Количество запросов  | 3"));
        assertTrue(md.contains("Not Modified | 3"));
    }

    //    @Test
    //    @DisplayName("Сохранение статистики в формате ADOC")
    //    void adocTest() {
    //        fail("Not implemented yet");  // Optional, keep fail
    //    }
}
