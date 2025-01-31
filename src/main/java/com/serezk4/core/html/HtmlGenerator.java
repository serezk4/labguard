package com.serezk4.core.html;

import com.serezk4.core.lab.model.Clazz;
import com.serezk4.core.lab.model.Lab;
import com.serezk4.core.lab.model.Plagiarist;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Utility class for generating HTML reports for plagiarism detection results.
 *
 * <p>
 * The {@code HtmlGenerator} class provides methods to create a detailed HTML report that includes:
 * <ul>
 *     <li>Checkstyle warnings for the analyzed lab.</li>
 *     <li>Plagiarism comparisons with other labs.</li>
 *     <li>Interactive features, such as toggling comparisons and sorting results.</li>
 * </ul>
 * The generated report is saved as an HTML file in the current working directory.
 * </p>
 *
 * <p><b>Usage:</b></p>
 * <pre>{@code
 * HtmlGenerator.generateHtmlReport(isu, labNumber, labs, results);
 * }</pre>
 *
 * @see Lab
 * @see Clazz
 * @see Plagiarist
 * @see <a href="https://highlightjs.org/">Highlight.js</a>
 *
 * @author serezk4
 * @version 1.0
 * @since 1.0
 */
public class HtmlGenerator {

    /**
     * Generates an HTML report for the specified lab and plagiarism detection results.
     *
     * <p>
     * The report includes:
     * <ul>
     *     <li>Checkstyle warnings for the target lab.</li>
     *     <li>Plagiarism comparisons with other students' labs, highlighting similarities.</li>
     *     <li>Code blocks and visual elements for easier analysis.</li>
     * </ul>
     * The report is saved as {@code plagiarism_report.html} in the current directory.
     * </p>
     *  @param isu       The ISU identifier of the student whose lab is being analyzed.
     *
     * @param labNumber The lab number being analyzed.
     * @param labs      A list of {@link Lab} objects representing all available labs.
     * @param targetLab
     * @param results   A map where the key is another student's ISU identifier, and the value is a list of
     *                  {@link Plagiarist} objects representing detected plagiarism cases.
     */
    public static void generateHtmlReport(
            final String isu,
            final int labNumber,
            final List<Lab> labs,
            final Lab targetLab,
            final Map<String, List<Plagiarist>> results
    ) {
        StringBuilder htmlBuilder = new StringBuilder();
        htmlBuilder.append("<!DOCTYPE html>")
                .append("<html lang=\"en\">")
                .append("<head>")
                .append("<meta charset=\"UTF-8\">")
                .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">")
                .append("<title>Plagiarism Report</title>")
                .append("<link rel=\"stylesheet\" href=\"https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.7.0/styles/default.min.css\">")
                .append("<script src=\"https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.7.0/highlight.min.js\"></script>")
                .append("<script>hljs.highlightAll();</script>")
                .append("<style>")
                .append("body { font-family: Arial, sans-serif; margin: 20px; }")
                .append("table { border-collapse: collapse; width: 100%; margin-bottom: 20px; }")
                .append("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }")
                .append("th { background-color: #f2f2f2; cursor: pointer; }")
                .append("tr:nth-child(even) { background-color: #f9f9f9; }")
                .append(".highlight { background-color: #ffcccc; }")
                .append(".code-block { background-color: #f4f4f4; padding: 10px; max-width: 50%; border: 1px solid #ddd; font-family: monospace; white-space: pre; overflow-x: scroll; max-height: 40em; width: 50%; overflow-y: scroll; box-sizing: border-box; }")
                .append(".hidden { display: none; }")
                .append(".toggle-button { cursor: pointer; color: blue; text-decoration: underline; }")
                .append(".comparison { display: flex; gap: 20px; justify-content: space-between; }")
                .append(".added { background-color: #e6ffed; }")
                .append(".removed { background-color: #ffecec; }")
                .append(".unchanged { background-color: #ffffff; }")
                .append("</style>")
                .append("<script>")
                .append("function toggleVisibility(id) { const elem = document.getElementById(id); elem.classList.toggle('hidden'); }")
                .append("function sortTable(n) {")
                .append("    const table = document.getElementById('plagiarismTable');")
                .append("    let rows = Array.from(table.rows).slice(1);")
                .append("    const ascending = table.getAttribute('data-sort-asc') === 'true';")
                .append("    rows.sort((a, b) => parseFloat(a.cells[n].textContent) - parseFloat(b.cells[n].textContent) * (ascending ? 1 : -1));")
                .append("    rows.forEach(row => table.appendChild(row));")
                .append("    table.setAttribute('data-sort-asc', !ascending);")
                .append("}")
                .append("</script>")
                .append("</head>")
                .append("<body>")
                .append("<h1>Plagiarism Report</h1>")
                .append(String.format("<h2>ISU: %s, Lab: %d</h2>", isu, labNumber));

        // Checkstyle Warnings Section
        htmlBuilder.append("<h2>Checkstyle Warnings</h2>");
        targetLab.clazzes().forEach(clazz -> {
            if (clazz.checkstyle().isEmpty()) return;
            htmlBuilder.append(String.format("<h3>Class: %s</h3>", clazz.name()));
            htmlBuilder.append("<ul>");
            clazz.checkstyle().forEach(warning -> htmlBuilder.append("<li>").append(escapeHtml(warning)).append("</li>"));
            htmlBuilder.append("</ul>");
        });

        // Plagiarism Comparisons Section
        for (Map.Entry<String, List<Plagiarist>> entry : results.entrySet()) {
            String otherIsu = entry.getKey();
            List<Plagiarist> plagiarists = entry.getValue();

            htmlBuilder.append(String.format("<h3>Compared with ISU: %s <a href=\"https://my.itmo.ru/persons/%s\">my.itmo</a></h3>", otherIsu, otherIsu));
            if (plagiarists.isEmpty()) {
                htmlBuilder.append("<p>No plagiarism detected.</p>");
                continue;
            }

            htmlBuilder.append("<table id='plagiarismTable' data-sort-asc='true'>")
                    .append("<tr>")
                    .append("<th onclick='sortTable(0)'>Target Class</th>")
                    .append("<th onclick='sortTable(1)'>Source Class</th>")
                    .append("<th onclick='sortTable(2)'>Similarity</th>")
                    .append("<th>Details</th>")
                    .append("</tr>");

            for (int i = 0; i < plagiarists.size(); i++) {
                Plagiarist plagiarist = plagiarists.get(i);
                String rowClass = plagiarist.similarity() > 0.7 ? "class=\"highlight\"" : "";
                String comparisonId = "comparison-" + otherIsu + "-" + i;

                htmlBuilder.append(String.format(
                        "<tr %s><td>%s</td><td>%s</td><td>%.2f</td><td><span class=\"toggle-button\" onclick=\"toggleVisibility('%s')\">Show Comparison</span></td></tr>",
                        rowClass,
                        plagiarist.targetClazz().name(),
                        plagiarist.plagiarizedClazz().name(),
                        plagiarist.similarity(),
                        comparisonId
                ));

                htmlBuilder.append(String.format(
                        "<tr id='%s' class='hidden'><td colspan='4'><div class='comparison'>" +
                                "<div class='code-block'><pre><code class='language-java'>%s</code></pre></div>" +
                                "<div class='code-block'><pre><code class='language-java'>%s</code></pre></div></div></td></tr>",
                        comparisonId,
                        escapeHtml(plagiarist.targetClazz().source()),
                        escapeHtml(plagiarist.plagiarizedClazz().source())
                ));
            }

            htmlBuilder.append("</table>").append("<br>");
        }

        htmlBuilder.append("</body>").append("</html>");

        Path reportPath = Path.of("plagiarism_report.html");
        try {
            Files.writeString(reportPath, htmlBuilder.toString());
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
        System.out.println("HTML report generated: " + reportPath.toAbsolutePath());
    }

    /**
     * Escapes HTML special characters in the input string to prevent rendering issues or XSS attacks.
     *
     * <p>
     * This method replaces characters such as {@code <}, {@code >}, {@code &}, {@code "} and {@code '} with their
     * corresponding HTML entities.
     * </p>
     *
     * @param input The raw string to escape.
     * @return A sanitized string safe for inclusion in HTML content.
     */
    private static String escapeHtml(String input) {
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    /**
     * Highlights differences between two code snippets.
     *
     * <p>
     * This method compares two code snippets line by line and wraps added, removed, or unchanged lines
     * in corresponding HTML elements for visual distinction. The result is suitable for embedding in an HTML document.
     * </p>
     *
     * @param source1  The first code snippet to compare (typically the target class).
     * @param source2  The second code snippet to compare (typically the plagiarized class).
     * @param isTarget A flag indicating whether the comparison is for the target class.
     *                 If {@code true}, differences are marked as removed; otherwise, they are marked as added.
     * @return A string containing the highlighted differences, formatted as HTML.
     */
    private static String highlightDifferences(String source1, String source2, boolean isTarget) {
        String[] lines1 = source1.split("\n");
        String[] lines2 = source2.split("\n");
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < Math.max(lines1.length, lines2.length); i++) {
            String line1 = i < lines1.length ? lines1[i] : "";
            String line2 = i < lines2.length ? lines2[i] : "";
            if (line1.equals(line2)) {
                result.append(escapeHtml(line1)).append("\n");
            } else if (isTarget) {
                result.append("<span class='removed'>").append(escapeHtml(line1)).append("</span>\n");
            } else {
                result.append("<span class='added'>").append(escapeHtml(line2)).append("</span>\n");
            }
        }
        return result.toString();
    }
}
