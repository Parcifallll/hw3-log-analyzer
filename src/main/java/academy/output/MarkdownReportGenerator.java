package academy.output;

import academy.model.CodeCount;
import academy.model.ResponseSize;
import academy.model.Stats;
import academy.model.TopResource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;

public class MarkdownReportGenerator implements ReportGenerator {

    private static final DecimalFormat df = new DecimalFormat("#.##");  // 2 decimal digits

    @Override
    public void generate(Stats stats, Path outputPath) {
        StringBuilder md = new StringBuilder();

        // General info
        md.append("# Log Analysis Report\n\n");
        md.append("## General Statistics\n\n");
        md.append("Files: ").append(stats.files()).append("\n\n");
        md.append("Total Requests: ").append(stats.totalRequestsCount()).append("\n\n");
        ResponseSize size = stats.responseSizeInBytes();
        md.append("Response Size (bytes):\n");
        md.append("- Average: ").append(df.format(size.average())).append("\n");
        md.append("- Max: ").append(df.format(size.max())).append("\n");
        md.append("- P95: ").append(df.format(size.p95())).append("\n\n");

        // Top resources table
        md.append("## Top 10 Resources\n\n");
        md.append("| Resource | Count |\n");
        md.append("|----------|-------|\n");
        for (TopResource tr : stats.resources()) {
            md.append("| ").append(tr.resource()).append(" | ").append(tr.totalRequestsCount()).append(" |\n");
        }
        md.append("\n");

        // Response codes table
        md.append("## Response Codes\n\n");
        md.append("| Code | Count |\n");
        md.append("|------|-------|\n");
        for (CodeCount cc : stats.responseCodes()) {
            md.append("| ").append(cc.code()).append(" | ").append(cc.totalResponsesCount()).append(" |\n");
        }

        try {
            Files.writeString(outputPath, md.toString());
        } catch (IOException e) {
            throw new RuntimeException("Failed to write Markdown report: " + outputPath, e);
        }
    }
}
