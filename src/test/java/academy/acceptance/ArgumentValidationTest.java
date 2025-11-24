package academy.acceptance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

public class ArgumentValidationTest {

    private static final String JAR_PATH = "target/hw3-logs-1.0.jar";

    private String getError(Process process) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        return reader.lines().collect(Collectors.joining("\n"));
    }

    private String getStderr(Process process) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        return reader.lines().collect(Collectors.joining("\n"));
    }

    @Test
    @DisplayName("На вход передан несуществующий локальный файл")
    void test1() throws IOException, InterruptedException {
        Process process = new ProcessBuilder(
                        "java",
                        "-jar",
                        JAR_PATH,
                        "--path",
                        "nonexistent.log",
                        "--format",
                        "json",
                        "--output",
                        "report.json")
                .start();
        process.waitFor();
        assertEquals(2, process.exitValue());
        String error = getError(process);
        assertTrue(error.contains("File not found: nonexistent.log"));
    }

    @Test
    @DisplayName("На вход передан несуществующий удаленный файл")
    void test2() throws IOException, InterruptedException {
        Process process = new ProcessBuilder(
                        "java",
                        "-jar",
                        JAR_PATH,
                        "--path",
                        "https://example.com/nonexistent.log",
                        "--format",
                        "json",
                        "--output",
                        "report.json")
                .start();
        process.waitFor();
        assertEquals(2, process.exitValue());
        String error = getError(process);
        assertTrue(error.contains("Remote file not found (404)"));
    }

    @ParameterizedTest
    @ValueSource(strings = {".docx"})
    @DisplayName("На вход передан файл в неподдерживаемом формате")
    void test3(String extension) throws IOException, InterruptedException {
        Path tempFile = Files.createTempFile("test", extension);
        Process process = new ProcessBuilder(
                        "java",
                        "-jar",
                        JAR_PATH,
                        "--path",
                        tempFile.toString(),
                        "--format",
                        "json",
                        "--output",
                        "report.json")
                .start();
        process.waitFor();
        assertEquals(2, process.exitValue());
        String error = getError(process);
        assertTrue(error.contains("Unsupported file format"));
        Files.delete(tempFile);
    }

    @ParameterizedTest
    @ValueSource(strings = {"2025.01.01 10:30", "today", "10-31-2025", " ", ""})
    @DisplayName("На вход переданы невалидные параметры --from / --to - {0}")
    void test4(String from) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(
                        "java",
                        "-jar",
                        JAR_PATH,
                        "--path",
                        "logs/log1.log",
                        "--format",
                        "json",
                        "--output",
                        "report.json",
                        "--from",
                        from)
                .start();
        process.waitFor();
        assertEquals(2, process.exitValue());
        String error = getError(process);
        assertTrue(error.contains("Invalid format for --from"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"txt"})
    @DisplayName("Результаты запрошены в неподдерживаемом формате {0}")
    void test5(String format) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(
                        "java",
                        "-jar",
                        JAR_PATH,
                        "--path",
                        "logs/log1.log",
                        "--format",
                        format,
                        "--output",
                        "report.txt")
                .start();
        process.waitFor();
        assertEquals(2, process.exitValue());
        String error = getError(process);
        assertTrue(error.contains("Unsupported format: " + format));
    }

    @ParameterizedTest
    @MethodSource("test6ArgumentsSource")
    @DisplayName("По пути в аргументе --output указан файл с некоректным расширением")
    void test6(String format, String output) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(
                        "java", "-jar", JAR_PATH, "--path", "logs/log1.log", "--format", format, "--output", output)
                .start();
        process.waitFor();
        assertEquals(2, process.exitValue());
        String error = getError(process);
        assertTrue(error.contains("Output file extension must be"));
    }

    @Test
    @DisplayName("По пути в аргументе --output уже существует файл")
    void test7() throws IOException, InterruptedException {
        Path tempOutput = Files.createTempFile("report", ".json");
        Process process = new ProcessBuilder(
                        "java",
                        "-jar",
                        JAR_PATH,
                        "--path",
                        "logs/log1.log",
                        "--format",
                        "json",
                        "--output",
                        tempOutput.toString())
                .start();
        process.waitFor();
        assertEquals(2, process.exitValue());
        String error = getError(process);
        assertTrue(error.contains("Output file already exists"));
        Files.delete(tempOutput);
    }

    @ParameterizedTest
    @ValueSource(strings = {"--path", "--output", "--format"})
    @DisplayName("На вход не передан обязательный параметр \"{0}\"")
    void test8(String argument) throws IOException, InterruptedException {
        List<String> args = new ArrayList<>(Arrays.asList(
                "java", "-jar", JAR_PATH, "--path", "logs/log1.log", "--format", "json", "--output", "report.json"));
        args.remove(argument); // Remove one required
        Process process = new ProcessBuilder(args).start();
        process.waitFor();
        assertEquals(2, process.exitValue());
        String error = getStderr(process);
        assertTrue(error.contains("Missing required option"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"--input", "--filter"})
    @DisplayName("На вход передан неподдерживаемый параметр \"{0}\"")
    void test9(String argument) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(
                        "java",
                        "-jar",
                        JAR_PATH,
                        "--path",
                        "logs/log1.log",
                        "--format",
                        "json",
                        "--output",
                        "report.json",
                        argument)
                .start();
        process.waitFor();
        assertEquals(2, process.exitValue());
        String error = getStderr(process);
        assertTrue(error.contains("Unknown option: '" + argument + "'"));
    }

    @Test
    @DisplayName("Значение параметра --from больше, чем значение параметра --to")
    void test10() throws IOException, InterruptedException {
        Process process = new ProcessBuilder(
                        "java",
                        "-jar",
                        JAR_PATH,
                        "--path",
                        "logs/log1.log",
                        "--format",
                        "json",
                        "--output",
                        "report2.json",
                        "--from",
                        "2025-12-01",
                        "--to",
                        "2025-01-01")
                .start();
        process.waitFor();
        assertEquals(2, process.exitValue());
        String error = getError(process);
        System.out.println("err: " + error);
        assertTrue(error.contains("FROM date must be before TO date"));
    }

    private static Stream<Arguments> test6ArgumentsSource() {
        return Stream.of(Arguments.of("markdown", "./results.txt"), Arguments.of("json", "./results.md"));
        // Arguments.of("adoc", "./results.ad1"));
    }
}
