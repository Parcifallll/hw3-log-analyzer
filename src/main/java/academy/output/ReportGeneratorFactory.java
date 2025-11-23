package academy.output;

// factory for creating ReportGenerator based on format
public class ReportGeneratorFactory {

    public static ReportGenerator create(String format) {
        return switch (format.toLowerCase()) {
            case "json" -> new JsonReportGenerator();
            case "markdown" -> new MarkdownReportGenerator();
            default -> throw new IllegalArgumentException("Unsupported format: " + format);
        };
    }
}
