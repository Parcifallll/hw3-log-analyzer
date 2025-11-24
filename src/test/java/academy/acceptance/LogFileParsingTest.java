package academy.acceptance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import academy.model.Stats;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
    void localFileProcessingTest() throws IOException, InterruptedException {
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

        String error = getError(process);
        assertTrue(error.contains("Invalid log format"));
        ObjectMapper mapper = new ObjectMapper();
        Stats stats = mapper.readValue(tempOutput.toFile(), Stats.class);
        assertEquals(2, stats.totalRequestsCount());
    }

    @Test
    @DisplayName("На вход передан валидный удаленный log-файл")
    void remoteFileProcessingTest() throws IOException, InterruptedException {
        Process process = new ProcessBuilder(
                        "java",
                        "-jar",
                        JAR_PATH,
                        "--path",
                        "https://raw.githubusercontent.com/elastic/examples/master/Common%20Data%20Formats/nginx_logs/nginx_logs",
                        "--format",
                        "json",
                        "--output",
                        tempOutput.toString())
                .start();
        process.waitFor();
        assertEquals(0, process.exitValue());

        ObjectMapper mapper = new ObjectMapper();
        Stats stats = mapper.readValue(tempOutput.toFile(), Stats.class);
        assertTrue(stats.totalRequestsCount() > 0);
    }

    @Test
    @DisplayName("На вход передан валидный локальный log-файл, "
            + "часть строк в котором нужно отфильтровать по --from и --to")
    void localFileProcessingAndFilteringTest() throws IOException, InterruptedException {
        // Add line with future date
        Files.writeString(
                tempLog,
                """
            93.180.71.3 - - [17/May/2015:08:05:32 +0000] "GET /downloads/product_1 HTTP/1.1" 304 0 "-" "Debian APT-HTTP/1.3 (0.8.16~exp12ubuntu10.21)"
            93.180.71.3 - - [17/May/2015:08:05:23 +0000] "GET /downloads/product_1 HTTP/1.1" 304 0 "-" "Debian APT-HTTP/1.3 (0.8.16~exp12ubuntu10.21)"
            80.91.33.133 - - [24/Nov/2025:08:05:24 +0000] "GET /downloads/product_1 HTTP/1.1" 304 0 "-" "Debian APT-HTTP/1.3 (0.8.16~exp12ubuntu10.17)"
            """);
        Process process = new ProcessBuilder(
                        "java",
                        "-jar",
                        JAR_PATH,
                        "--path",
                        tempLog.toString(),
                        "--format",
                        "json",
                        "--output",
                        tempOutput.toString(),
                        "--from",
                        "2015-05-01",
                        "--to",
                        "2015-05-31")
                .start();
        process.waitFor();
        assertEquals(0, process.exitValue());

        ObjectMapper mapper = new ObjectMapper();
        JavaTimeModule module = new JavaTimeModule();
        module.addDeserializer(LocalDate.class, new LocalDateDeserializer(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
        mapper.registerModule(module); // for LocalDate
        Stats stats = mapper.readValue(tempOutput.toFile(), Stats.class);
        assertEquals(2, stats.totalRequestsCount()); // Filtered, excluded 2025 date
    }

    @Test
    @DisplayName("На вход передан локальный log-файл, часть строк в котором не подходит под формат")
    void damagedLocalFileProcessingTest() throws IOException, InterruptedException {
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

        String error = getError(process);
        assertTrue(error.contains("Invalid log format"));
        ObjectMapper mapper = new ObjectMapper();
        Stats stats = mapper.readValue(tempOutput.toFile(), Stats.class);
        assertEquals(2, stats.totalRequestsCount()); // Skip invalid
    }

    private String getError(Process process) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        return reader.lines().collect(Collectors.joining("\n"));
    }
}
