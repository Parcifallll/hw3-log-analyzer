package academy.output;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import academy.model.Stats;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class JsonReportGenerator implements ReportGenerator {

    @Override
    public void generate(Stats stats, Path outputPath) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.registerModule(new JavaTimeModule()); // for LocalDate
        try {
            String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(stats);
            Files.writeString(outputPath, json);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write JSON report: " + outputPath, e);
        }
    }
}
