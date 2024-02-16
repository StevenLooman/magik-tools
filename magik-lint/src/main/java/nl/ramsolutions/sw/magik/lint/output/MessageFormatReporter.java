package nl.ramsolutions.sw.magik.lint.output;

import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import nl.ramsolutions.sw.magik.checks.MagikCheckHolder;
import nl.ramsolutions.sw.magik.checks.MagikCheckMetadata;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Message format reporter.
 */
public class MessageFormatReporter implements Reporter {

    /**
     * Default format.
     */
    public static final String DEFAULT_FORMAT = "${path}:${line}:${column}:${msg} (${symbol})";

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageFormatReporter.class);

    private final PrintStream outStream;
    private final String format;
    private final Long columnOffset;
    private final Set<String> reportedSeverities;

    /**
     * Constructor.
     *
     * @param outStream Output stream to write to.
     * @param format Format to use.
     * @param columnOffset Column offset for reported columns.
     */
    public MessageFormatReporter(final PrintStream outStream, final String format, final @Nullable Long columnOffset) {
        this.outStream = outStream;
        this.format = format;
        if (columnOffset != null) {
            this.columnOffset = columnOffset;
        } else {
            this.columnOffset = 0L;
        }
        this.reportedSeverities = new HashSet<>();
    }

    private Map<String, String> createMapForMagikIssue(final Path path, final MagikIssue issue) {
        final MagikCheckHolder holder = issue.check().getHolder();
        if (holder == null) {
            throw new IllegalStateException();
        }

        final Map<String, String> map = new HashMap<>();
        map.put("path", path.toString());
        map.put("abspath", path.toAbsolutePath().toString());
        map.put("msg", issue.message());
        try {
            final MagikCheckMetadata metadata = holder.getMetadata();
            map.put("msg_id", metadata.getSqKey());
            map.put("symbol", metadata.getSqKey());
            map.put("severity", metadata.getDefaultSeverity());
            map.put("category", metadata.getDefaultSeverity());

            final List<String> tags = metadata.getTags();
            final String tag = !tags.isEmpty()
                ? tags.get(0)
                : null;
            map.put("tag", tag);
        } catch (final IOException exception) {
            LOGGER.error("Could not find file: {}", exception.getMessage(), exception);
        }

        final Integer startLine = issue.startLine();
        if (startLine != null) {
            map.put("line", startLine.toString());
            map.put("start_line", startLine.toString());
        }
        Integer startColumn = issue.startColumn();
        if (startColumn != null) {
            startColumn += this.columnOffset.intValue();
            map.put("column", startColumn.toString());
            map.put("start_column", startColumn.toString());
        }

        final Integer endLine = issue.endLine();
        if (endLine != null) {
            map.put("end_line", endLine.toString());
        }
        Integer endColumn = issue.endColumn();
        if (endColumn != null) {
            endColumn += this.columnOffset.intValue();
            map.put("end_column", endColumn.toString());
        }

        return map;
    }

    @Override
    public void reportIssue(final MagikIssue magikIssue) {
        // Create map for find/replace.
        final Path path = magikIssue.location().getPath();
        final Map<String, String> map = this.createMapForMagikIssue(path, magikIssue);

        // Save severity.
        final String severity = map.get("severity");
        this.reportedSeverities.add(severity);

        // Do find/replace.
        String line = this.format;
        for (final Map.Entry<String, String> entry : map.entrySet()) {
            final String key = "${" + entry.getKey() + "}";
            if (line.contains(key)) {
                final String matchKey = Pattern.quote(key);
                final String value = entry.getValue();
                final String matchValue = Matcher.quoteReplacement(value);
                line = line.replaceAll(matchKey, matchValue);
            }
        }
        this.outStream.println(line);
    }

    @Override
    public Set<String> reportedSeverities() {
        return Collections.unmodifiableSet(this.reportedSeverities);
    }

}
