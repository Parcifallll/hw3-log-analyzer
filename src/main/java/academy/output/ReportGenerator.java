package academy.output;

import academy.model.Stats;
import java.nio.file.Path;

// generating reports in different formats
public interface ReportGenerator {
    void generate(Stats stats, Path outputPath);
}
