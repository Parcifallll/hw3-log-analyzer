package academy.acceptance;

import static org.junit.jupiter.api.Assertions.assertEquals;

import academy.cli.RunCommand;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import picocli.CommandLine;

public class ArgumentValidationTest {
    @Test
    @DisplayName("На вход передан несуществующий локальный файл")
    void test1() {
        String[] args = {"--format", "--path", "nofile.log", "--format", "json", "--output", "output.json"};
        int exitCode = new CommandLine(new RunCommand()).execute(args);
        assertEquals(2, exitCode);
    }

    @Test
    @DisplayName("На вход передан несуществующий удаленный файл")
    void test2() {
        String[] args = {"--path", "https://example.com/nonexistent.log", "--format", "json", "--output", "report.json"
        };
        int exitCode = new CommandLine(new RunCommand()).execute(args);
        assertEquals(2, exitCode);
    }

    @ParameterizedTest
    @ValueSource(strings = {".docx", ".ffsd"})
    @DisplayName("На вход передан файл в неподдерживаемом формате")
    void test3(String extension) throws IOException {
        Path tempFile = Files.createTempFile("test", extension);
        String[] args = {"--path", tempFile.toString(), "--format", "json", "--output", "report.json"};
        int exitCode = new CommandLine(new RunCommand()).execute(args);
        assertEquals(2, exitCode);
        Files.delete(tempFile);
    }

    @ParameterizedTest
    @ValueSource(strings = {"2025.01.01 10:30", "today", "10-31-2025", " ", ""})
    @DisplayName("На вход переданы невалидные параметры --from / --to - {0}")
    void test4(String from) {
        String[] args = {"--path", "logs/log1.log", "--format", "json", "--output", "report.json", "--from", from};
        int exitCode = new CommandLine(new RunCommand()).execute(args);
        assertEquals(2, exitCode);
    }

    @ParameterizedTest
    @ValueSource(strings = {"txt"})
    @DisplayName("Результаты запрошены в неподдерживаемом формате {0}")
    void test5(String format) {
        String[] args = {"--path", "logs/log1.log", "--format", format, "--output", "report.txt"};
        int exitCode = new CommandLine(new RunCommand()).execute(args);
        assertEquals(2, exitCode);
    }

    @ParameterizedTest
    @MethodSource("test6ArgumentsSource")
    @DisplayName("По пути в аргументе --output указан файл с некоректным расширением")
    void test6(String format, String output) {
        String[] args = {"--path", "logs/log1.log", "--format", format, "--output", output};
        int exitCode = new CommandLine(new RunCommand()).execute(args);
        assertEquals(2, exitCode);
    }

    @Test
    @DisplayName("По пути в аргументе --output уже существует файл")
    void test7() throws IOException {
        Path tempOutput = Files.createTempFile("report", ".json");
        String[] args = {"--path", "logs/log1.log", "--format", "json", "--output", tempOutput.toString()};
        int exitCode = new CommandLine(new RunCommand()).execute(args);
        assertEquals(2, exitCode);
        Files.delete(tempOutput);
    }

    @ParameterizedTest
    @ValueSource(strings = {"--path", "--output", "--format", "-p", "-o", "-f"})
    @DisplayName("На вход не передан обязательный параметр \"{0}\"")
    void test8(String argument) {
        List<String> argsList = new ArrayList<>(
                Arrays.asList("--path", "logs/log1.log", "--format", "json", "--output", "report.json"));
        argsList.remove(argument); // remove required
        String[] args = argsList.toArray(new String[0]);
        int exitCode = new CommandLine(new RunCommand()).execute(args);
        assertEquals(2, exitCode);
    }

    @ParameterizedTest
    @ValueSource(strings = {"--input", "--filter"})
    @DisplayName("На вход передан неподдерживаемый параметр \"{0}\"")
    void test9(String argument) {
        String[] args = {"--path", "logs/log1.log", "--format", "json", "--output", "report.json", argument};
        int exitCode = new CommandLine(new RunCommand()).execute(args);
        assertEquals(2, exitCode);
    }

    @Test
    @DisplayName("Значение параметра --from больше, чем значение параметра --to")
    void test10() {
        String[] args = {
            "--path",
            "logs/log1.log",
            "--format",
            "json",
            "--output",
            "report2.json",
            "--from",
            "2025-12-01",
            "--to",
            "2025-01-01"
        };
        int exitCode = new CommandLine(new RunCommand()).execute(args);
        assertEquals(2, exitCode);
    }

    private static Stream<Arguments> test6ArgumentsSource() {
        return Stream.of(Arguments.of("markdown", "./results.txt"), Arguments.of("json", "./results.md"));
        // Arguments.of("adoc", "./results.ad1"));
    }
}
