package nl.ramsolutions.sw.magik.lint;

import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.ConfigurationLocator;
import nl.ramsolutions.sw.FileCharsetDeterminer;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.analysis.scope.Scope;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import nl.ramsolutions.sw.magik.checks.MagikCheckHolder;
import nl.ramsolutions.sw.magik.checks.MagikChecksConfiguration;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import nl.ramsolutions.sw.magik.lint.output.Reporter;
import nl.ramsolutions.sw.magik.parser.CommentInstructionReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Magik Lint main class.
 */
public class MagikLint {

    private static final Logger LOGGER = LoggerFactory.getLogger(MagikLint.class);

    private static final String KEY_DISABLE = "disable";
    private static final CommentInstructionReader.InstructionType MLINT_LINE_INSTRUCTION =
        CommentInstructionReader.InstructionType.createStatementInstructionType("mlint");
    private static final CommentInstructionReader.InstructionType MLINT_SCOPE_INSTRUCTION =
        CommentInstructionReader.InstructionType.createScopeInstructionType("mlint");

    private final MagikLintConfiguration config;
    private final Reporter reporter;

    /**
     * Constructor, parses command line and reads configuration.
     *
     * @param configuration Configuration.
     * @param reporter            Reporter.
     */
    public MagikLint(final MagikLintConfiguration configuration, final Reporter reporter) {
        this.config = configuration;
        this.reporter = reporter;
    }

    /**
     * Build context for a file.
     * @param path Path to file
     * @return Visitor context for file.
     */
    private MagikFile buildMagikFile(final Path path) {
        final Charset charset = FileCharsetDeterminer.determineCharset(path);

        byte[] encoded = null;
        try {
            encoded = Files.readAllBytes(path);
        } catch (final IOException exception) {
            LOGGER.error(exception.getMessage(), exception);
        }

        final URI uri = path.toUri();
        final String fileContents = new String(encoded, charset);
        return new MagikFile(uri, fileContents);
    }

    /**
     * Run a single check on context.
     *
     * @param magikFile File to run check on.
     * @param holder MagikCheckHolder Check to run.
     * @return Issues/infractions found.
     * @throws ReflectiveOperationException -
     */
    private List<MagikIssue> runCheckOnFile(final MagikFile magikFile, final MagikCheckHolder holder)
            throws ReflectiveOperationException {
        final MagikCheck check = holder.createCheck();
        return check.scanFileForIssues(magikFile);
    }

    /**
     * Show checks active and inactive checks.
     *
     * @param writer Writer Write to write output to.
     * @param showDisabled boolean Boolean to show disabled checks or not.
     * @throws ReflectiveOperationException -
     * @throws IOException -
     */
    void showChecks(final Writer writer, final boolean showDisabled) throws ReflectiveOperationException, IOException {
        final Path configPath = this.config.getPath();
        final MagikChecksConfiguration checksConfig = configPath != null
            ? new MagikChecksConfiguration(configPath)
            : new MagikChecksConfiguration();
        final Iterable<MagikCheckHolder> holders = checksConfig.getAllChecks();
        for (final MagikCheckHolder holder : holders) {
            if (!showDisabled && holder.isEnabled() || showDisabled && !holder.isEnabled()) {
                writer.write("- " + holder.getSqKey() + " (" + holder.getTitle() + ")\n");
            } else {
                continue;
            }

            for (final MagikCheckHolder.Parameter parameter : holder.getParameters()) {
                writer.write("\t"
                    + parameter.getName() + ":\t"
                    + parameter.getValue() + " "
                    + "(" + parameter.getDescription() + ")\n");
            }
        }
    }

    /**
     * Show enabled checks.
     *
     * @param writer Writer Write to write output to.
     * @throws ReflectiveOperationException -
     * @throws IOException -
     */
    void showEnabledChecks(final Writer writer) throws ReflectiveOperationException, IOException {
        writer.write("Enabled checks:\n");
        this.showChecks(writer, false);
    }

    /**
     * Show disabled checks.
     *
     * @param writer Writer Write to write output to.
     * @throws ReflectiveOperationException -
     * @throws IOException -
     */
    void showDisabledChecks(final Writer writer) throws ReflectiveOperationException, IOException {
        writer.write("Disabled checks:\n");
        this.showChecks(writer, true);
    }

    /**
     * Check if a found issue/infraction is disabled via line or scope.
     * @param magikIssue Issue to check.
     * @param instructionsHandler Instruction handler to use.
     * @return true if issue is disabled at line.
     */
    private boolean isMagikIssueDisabled(final MagikFile magikFile, final MagikIssue magikIssue) {
        final MagikCheckHolder holder = magikIssue.check().getHolder();
        Objects.requireNonNull(holder);

        final Integer line = magikIssue.startLine();
        final Scope scope = magikFile.getGlobalScope().getScopeForLineColumn(line, Integer.MAX_VALUE);
        final Map<String, String> scopeInstructions =
            magikFile.getScopeInstructions(MLINT_SCOPE_INSTRUCTION).getOrDefault(scope, Collections.emptyMap());
        final Map<String, String> lineInstructions =
            magikFile.getLineInstructions(MLINT_LINE_INSTRUCTION).getOrDefault(line, Collections.emptyMap());
        final String[] scopeDisableds = scopeInstructions.getOrDefault(MagikLint.KEY_DISABLE, "").split(",");
        final String[] lineDisableds = lineInstructions.getOrDefault(MagikLint.KEY_DISABLE, "").split(",");
        final String checkKey = holder.getCheckKeyKebabCase();
        return List.of(scopeDisableds).contains(checkKey)
            || List.of(lineDisableds).contains(checkKey);
    }

    /**
     * Run {@link MagikCheckHolder}s on {@link MagikFile}.
     * @param magikFile File to run on.
     * @param holders {@link MagikCheckHolder}s to run.
     * @return List of {@link MagikIssue}s for the given file.
     */
    private List<MagikIssue> runChecksOnFile(final MagikFile magikFile, final Iterable<MagikCheckHolder> holders) {
        LOGGER.trace("Thread: {}, checking file: {}", Thread.currentThread().getName(), magikFile);

        final List<MagikIssue> magikIssues = new ArrayList<>();

        // run checks on files
        for (final MagikCheckHolder holder : holders) {
            if (!holder.isEnabled()) {
                continue;
            }

            try {
                final List<MagikIssue> issues = this.runCheckOnFile(magikFile, holder).stream()
                    .filter(magikIssue -> !this.isMagikIssueDisabled(magikFile, magikIssue))
                    .collect(Collectors.toList());
                magikIssues.addAll(issues);
            } catch (ReflectiveOperationException exception) {
                LOGGER.error(exception.getMessage(), exception);
            }
        }

        return magikIssues;
    }

    /**
     * Run the linter on {@code paths}.
     *
     * @throws IOException -
     * @throws ReflectiveOperationException -
     */
    public void run(final Collection<Path> paths) throws IOException, ReflectiveOperationException {
        final Path configPath = this.config.getPath();
        final MagikChecksConfiguration checksConfig = configPath != null
            ? new MagikChecksConfiguration(configPath)
            : new MagikChecksConfiguration();

        // Gather ignore matchers.
        final FileSystem fs = FileSystems.getDefault();
        final List<PathMatcher> ignoreMatchers = checksConfig.getIgnores().stream()
            .map(fs::getPathMatcher)
            .collect(Collectors.toList());
        final long maxInfractions = this.config.getMaxInfractions();

        // Sorting.
        final Location.LocationRangeComparator locationCompare = new Location.LocationRangeComparator();
        final Iterable<MagikCheckHolder> holders = checksConfig.getAllChecks();
        paths.stream()
            .parallel()
            .filter(path -> {
                final boolean matches = ignoreMatchers.stream()
                    .anyMatch(matcher -> matcher.matches(path));
                if (matches) {
                    LOGGER.trace("Thread: {}, ignoring file: {}", Thread.currentThread().getName(), path);
                }
                return !matches;
            })
            .map(path -> this.buildMagikFile(path))
            .map(magikFile -> this.runChecksOnFile(magikFile, holders))
            .flatMap(List::stream)
            // ensure ordering
            .collect(Collectors.toList())
            .stream()
            .sorted((issue0, issue1) -> locationCompare.compare(issue0.location(), issue1.location()))
            .limit(maxInfractions)
            .forEach(this.reporter::reportIssue);
    }

    /**
     * Run the linter on {@link MagikFile}.
     * @param magikFile File to run on.
     * @throws ReflectiveOperationException -
     * @throws IOException -
     */
    public void run(final MagikFile magikFile) throws ReflectiveOperationException, IOException {
        final URI uri = magikFile.getUri();
        final Path magikFilePath = Path.of(uri);
        final Path configPath = ConfigurationLocator.locateConfiguration(magikFilePath);
        final MagikChecksConfiguration checksConfig = configPath != null
            ? new MagikChecksConfiguration(configPath)
            : new MagikChecksConfiguration();
        final Iterable<MagikCheckHolder> holders = checksConfig.getAllChecks();
        final Comparator<MagikIssue> byLine = Comparator.comparing(MagikIssue::startLine);
        final Comparator<MagikIssue> byColumn = Comparator.comparing(MagikIssue::startColumn);
        this.runChecksOnFile(magikFile, holders).stream()
            .sorted(byLine.thenComparing(byColumn))
            .forEach(this.reporter::reportIssue);
    }

}
