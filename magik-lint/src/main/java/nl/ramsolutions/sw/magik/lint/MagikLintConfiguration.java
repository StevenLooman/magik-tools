package nl.ramsolutions.sw.magik.lint;

import java.io.IOException;
import java.nio.file.Path;
import javax.annotation.CheckForNull;
import nl.ramsolutions.sw.MagikToolsProperties;

/**
 * {@link MagikLint} specific configuration.
 */
public class MagikLintConfiguration {

    private static final String KEY_MAX_INFRACTIONS = "max-infractions";
    private static final String KEY_COLUMN_OFFSET = "column-offset";
    private static final String KEY_MSG_TEMPLATE = "msg-template";

    private final Path path;
    private final MagikToolsProperties properties;

    /**
     * Constructor.
     * @throws IOException
     */
    public MagikLintConfiguration() throws IOException {
        this.path = null;
        this.properties = new MagikToolsProperties();
    }

    /**
     * Constructor which reads properties from {@code path}.
     * @param path {@link Path} to read properties from.
     * @throws IOException
     */
    public MagikLintConfiguration(final Path path) throws IOException {
        this.path = path;
        this.properties = new MagikToolsProperties(path);
    }

    @CheckForNull
    public Path getPath() {
        return this.path;
    }

    /**
     * Get max infractions.
     * @return Max infractions.
     */
    public long getMaxInfractions() {
        final Long maxInfractions = this.properties.getPropertyLong(KEY_MAX_INFRACTIONS);
        if (maxInfractions == null) {
            return Long.MAX_VALUE;
        }

        return maxInfractions;
    }

    public void setMaxInfractions(final Long maxInfractions) {
        this.properties.setProperty(KEY_MAX_INFRACTIONS, maxInfractions);
    }

    /**
     * Get column offset.
     * @return Column offset.
     */
    public Long getColumnOffset() {
        final Long columnOffset = this.properties.getPropertyLong(KEY_COLUMN_OFFSET);
        if (columnOffset == null) {
            return 0L;
        }

        return columnOffset;
    }

    public void setColumnOffset(final Long columnOffset) {
        this.properties.setProperty(KEY_COLUMN_OFFSET, columnOffset);
    }

    /**
     * Get reporter format.
     * @return Reporter format.
     */
    @CheckForNull
    public String getReporterFormat() {
        return this.properties.getPropertyString(KEY_MSG_TEMPLATE);
    }

    public void setReporterFormat(final String reporterFormat) {
        this.properties.setProperty(KEY_MSG_TEMPLATE, reporterFormat);
    }

}
