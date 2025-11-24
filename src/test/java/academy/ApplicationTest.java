package academy;

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

public class ApplicationTest {
    private Path tempLog;
    private Path tempOutput;

    @BeforeEach
    void setUp() throws IOException {
        tempLog = Files.createTempFile("test", ".log"); // Temp Path
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
    @DisplayName("Базовая проверка работоспособности программы")
    void happyPathTest() {
        String[] args = {
            "--path", tempLog.toString(),
            "--format", "json",
            "--output", tempOutput.toString()
        };

        CommandLine cmd = new CommandLine(new RunCommand());
        int exitCode = cmd.execute(args);
        assertEquals(0, exitCode);
    }
}
