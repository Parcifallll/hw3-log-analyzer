package academy.cli;

import academy.model.Stats;
import academy.output.ReportGenerator;
import academy.output.ReportGeneratorFactory;
import academy.parser.LogParser;
import academy.parser.NginxLogParser;
import academy.reader.LocalLogReader;
import academy.reader.LogReader;
import academy.reader.LogReaderFactory;
import academy.stats.DateFilter;
import academy.stats.StatsCollector;
import academy.validation.ArgumentValidation;
import academy.validation.InvalidArgumentException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
        name = "hw3-log-analyzer",
        mixinStandardHelpOptions = true,
        version = "1.0",
        description = "Analyzes log files and generates reports")
public class RunCommand implements Callable<Integer> {

    private static final Logger logger = LogManager.getLogger(RunCommand.class);
    private static final int SUCCESS_CODE = 0;
    private static final int UNEXPECTED_ERROR_CODE = 1;
    private static final int INVALID_USAGE_CODE = 2;

    @Option(
            names = {"--path", "-p"},
            required = true,
            split = ",",
            description = "Path to log files or URL")
    private String[] paths;

    @Option(
            names = {"--format", "-f"},
            required = true,
            description = "Output format")
    private String format;

    @Option(
            names = {"--output", "-o"},
            required = true,
            description = "Output file path")
    private Path output;

    @Option(names = "--from", description = "Start date: yyyy-MM-dd")
    private String fromStr;

    @Option(names = "--to", description = "End date: yyyy-MM-dd")
    private String toStr;

    private LocalDate from; // Parsed in validator
    private LocalDate to;

    @Override
    public Integer call() {
        try {
            logger.info("Run log analysis");

            ArgumentValidation validator = new ArgumentValidation();
            validator.validate(this); // Throws InvalidArgumentException if invalid

            // Prepare components
            LogParser parser = new NginxLogParser();
            StatsCollector collector = new StatsCollector();
            DateFilter filter = new DateFilter(from, to);
            ReportGenerator generator = ReportGeneratorFactory.create(format);

            // Process each path
            Stream.of(paths)
                    .flatMap(path -> {
                        List<LogReader> readers = LogReaderFactory.createReaders(path);
                        return readers.stream().flatMap(reader -> {
                            String fileName;
                            if (reader instanceof LocalLogReader) {
                                fileName = ((LocalLogReader) reader)
                                        .filePath()
                                        .getFileName()
                                        .toString(); // glob/single
                            } else {
                                fileName = path.substring(
                                        path.lastIndexOf('/') + 1); // For remote URL, extract filename or "nginx_logs"
                            }
                            int[] lineNum = {1}; // Mutable for count (lines)
                            return reader.readLines().map(line -> parser.parseLine(line, fileName, lineNum[0]++));
                        });
                    })
                    .filter(log -> log != null)
                    .filter(filter::isWithinRange)
                    .forEach(collector::collect); // Collect stats

            // Get stats (files from paths, but resolve to actual if needed)
            Stats stats = collector.getStats(Arrays.asList(paths), from, to); // Pass original paths as files

            // Generate report
            generator.generate(stats, output);

            logger.info("Analysis completed.");
            return SUCCESS_CODE;
        } catch (InvalidArgumentException e) {
            logger.error("Invalid usage: {}", e.getMessage());
            return INVALID_USAGE_CODE;
        } catch (Exception e) {
            logger.error("Unexpected error: {}", e.getMessage(), e);
            return UNEXPECTED_ERROR_CODE;
        }
    }

    // getters for validation
    public String[] getPaths() {
        return paths;
    }

    public String getFormat() {
        return format;
    }

    public Path getOutput() {
        return output;
    }

    public LocalDate getFrom() {
        return from;
    }

    public LocalDate getTo() {
        return to;
    }

    public String getFromStr() {
        return fromStr;
    }

    public String getToStr() {
        return toStr;
    }

    public void setFrom(LocalDate parsedFrom) {
        this.from = parsedFrom;
    }

    public void setTo(LocalDate parsedTo) {
        this.to = parsedTo;
    }
}
