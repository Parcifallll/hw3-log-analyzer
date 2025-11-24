package academy.acceptance;

import static org.junit.jupiter.api.Assertions.assertEquals;

import academy.cli.RunCommand;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

public class LogFileParsingTest {

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
                invalid line
                """);
        tempOutput = Path.of("rep.json");
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(tempLog);
        Files.deleteIfExists(tempOutput);
    }

    @Test
    @DisplayName("На вход передан валидный локальный log-файл")
    void localFileProcessingTest() {
        String[] args = {"--path", tempLog.toString(), "--format", "json", "--output", tempOutput.toString()};
        int exitCode = new CommandLine(new RunCommand()).execute(args);
        assertEquals(0, exitCode);
    }

    @Test
    @DisplayName("На вход передан валидный удаленный log-файл")
    void remoteFileProcessingTest() {
        String[] args = {
            "--path",
            "https://raw.githubusercontent.com/elastic/examples/master/Common%20Data%20Formats/nginx_logs/nginx_logs",
            "--format",
            "json",
            "--output",
            tempOutput.toString()
        };
        int exitCode = new CommandLine(new RunCommand()).execute(args);
        assertEquals(0, exitCode);
    }

    @Test
    @DisplayName("На вход передан валидный локальный log-файл, "
            + "часть строк в котором нужно отфильтровать по --from и --to")
    void localFileProcessingAndFilteringTest() throws IOException, InterruptedException {
        Files.writeString(
                tempLog,
                "222.222.222.222 - - [20/Nov/2025:20:45:22 +0000] \"GET /downloads/game HTTP/1.1\" 304 0 \"-\" \"Test-Class/1.0)\"");

        String[] args = {
            "--path", tempLog.toString(),
            "--format", "json",
            "--output", tempOutput.toString(),
            "--from", "2025-05-22",
            "--to", "2025-05-31"
        };

        int exitCode = new CommandLine(new RunCommand()).execute(args);

        assertEquals(0, exitCode);
    }

    @Test
    @DisplayName("На вход передан локальный log-файл, часть строк в котором не подходит под формат")
    void damagedLocalFileProcessingTest() {
        String[] args = {
            "--path", tempLog.toString(),
            "--format", "json",
            "--output", tempOutput.toString()
        };

        int exitCode = new CommandLine(new RunCommand()).execute(args);
        assertEquals(0, exitCode);
    }
}
