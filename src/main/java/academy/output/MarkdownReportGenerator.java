package academy.output;

import academy.model.CodeCount;
import academy.model.ResponseSize;
import academy.model.Stats;
import academy.model.TopResource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

// Generates Markdown report with tables
public class MarkdownReportGenerator implements ReportGenerator {

    private static final DecimalFormat thousands = new DecimalFormat("#,###");  // with _
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private static final Map<Integer, String> CODE_NAMES = new HashMap<>();
    static {
        CODE_NAMES.put(200, "OK");
        CODE_NAMES.put(304, "Not Modified");
        CODE_NAMES.put(404, "Not Found");
        CODE_NAMES.put(500, "Internal Server Error");
    }

    @Override
    public void generate(Stats stats, Path outputPath) {
        StringBuilder md = new StringBuilder();

        // General info
        md.append("#### Общая информация\n\n");
        md.append("|        Метрика        |     Значение |\n");
        md.append("|:---------------------:|-------------:|\n");
        md.append("|       Файл(-ы)        | `").append(String.join(", ", stats.files())).append("` |\n");
        md.append("|    Начальная дата     | ").append(stats.from() != null ? stats.from().format(dateFormatter) : "-").append(" |\n");
        md.append("|     Конечная дата     | ").append(stats.to() != null ? stats.to().format(dateFormatter) : "-").append(" |\n");
        md.append("|  Количество запросов  | ").append(thousands.format(stats.totalRequestsCount())).append(" |\n");
        ResponseSize size = stats.responseSizeInBytes();
        md.append("| Средний размер ответа | ").append(size.average()).append("b |\n");
        md.append("|  95p размера ответа   | ").append(size.p95()).append("b |\n\n");

        // top resources table
        md.append("#### Запрашиваемые ресурсы\n\n");
        md.append("|     Ресурс      | Количество |\n");
        md.append("|:---------------:|-----------:|\n");
        for (TopResource tr : stats.resources()) {
            md.append("|  `").append(tr.resource()).append("`  | ").append(thousands.format(tr.totalRequestsCount())).append(" |\n");
        }
        md.append("\n");

        // Response codes table
        md.append("#### Коды ответа\n\n");
        md.append("| Код |          Имя          | Количество |\n");
        md.append("|:---:|:---------------------:|-----------:|\n");
        for (CodeCount cc : stats.responseCodes()) {
            String name = CODE_NAMES.getOrDefault(cc.code(), "Unknown");
            md.append("| ").append(cc.code()).append(" | ").append(name).append(" | ").append(thousands.format(cc.totalResponsesCount())).append(" |\n");
        }

        try {
            Files.writeString(outputPath, md.toString());
        } catch (IOException e) {
            throw new RuntimeException("Failed to write Markdown report: " + outputPath, e);
        }
    }
}
