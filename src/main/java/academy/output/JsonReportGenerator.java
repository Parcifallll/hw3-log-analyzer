package academy.output;

import academy.model.Stats;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class JsonReportGenerator implements ReportGenerator {
    @Override
    public void generate(Stats stats, Path outputPath) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        JavaTimeModule module = new JavaTimeModule();
        module.addSerializer(LocalDate.class, new LocalDateSerializer(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
        mapper.registerModule(module);
        try {
            String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(stats);
            Files.writeString(outputPath, json);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write JSON report: " + outputPath, e);
        }
    }
}
