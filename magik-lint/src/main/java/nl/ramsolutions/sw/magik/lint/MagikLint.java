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
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import nl.ramsolutions.sw.FileCharsetDeterminer;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.analysis.Location;
import nl.ramsolutions.sw.magik.checks.CheckList;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import nl.ramsolutions.sw.magik.checks.MagikCheckHolder;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import nl.ramsolutions.sw.magik.lint.output.Reporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;

/**
 * Magik Lint main class.
 */
public class MagikLint {

    private static final Logger LOGGER = LoggerFactory.getLogger(MagikLint.class);

    private final Configuration config;
    private final Reporter reporter;

    /**
     * Constructor, parses command line and reads configuration.
     *
     * @param configuration Configuration.
     * @param reporter            Reporter.
     */
    public MagikLint(final Configuration configuration, final Reporter reporter) {
        this.config = configuration;
        this.reporter = reporter;
    }

    /**
     * Build context for a file, untabifying if needed.
     *
     * @param path         Path to file
     * @param untabify Untabify to N-spaces, if given
     * @return Visitor context for file.
     */
    private MagikFile buildMagikFile(final Path path, final @Nullable Long untabify) {
        final Charset charset = FileCharsetDeterminer.determineCharset(path);

        byte[] encoded = null;
        try {
            encoded = Files.readAllBytes(path);
        } catch (IOException exception) {
            LOGGER.error(exception.getMessage(), exception);
        }

        final URI uri = path.toUri();

        String fileContents = new String(encoded, charset);
        if (untabify != null) {
            final String formatString = "%" + untabify + "s";
            final String spaces = String.format(formatString, "");
            fileContents = fileContents.replace("\t", spaces);
        }

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
    private List<MagikIssue> runCheckOnFile(
            final MagikFile magikFile, final MagikCheckHolder holder) throws ReflectiveOperationException {
        final MagikCheck check = holder.createCheck();
        return check.scanFileForIssues(magikFile);
    }

    /**
     * Show checks active and inactive checks.
     *
     * @throws ReflectiveOperationException -
     * @throws IOException -
     */
    void showChecks(final Writer writer) throws ReflectiveOperationException, IOException {
        final Iterable<MagikCheckHolder> holders = MagikLint.getAllChecks(this.config);
        for (final MagikCheckHolder holder : holders) {
            final String name = holder.getSqKey();
            if (holder.isEnabled()) {
                writer.write("Check: " + name + " (" + holder.getTitle() + ")\n");
            } else {
                writer.write("Check: " + name + " (disabled) (" + holder.getTitle() + ")\n");
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
     * Check if a found issue/infraction is disabled via line or scope.
     * @param magikIssue Issue to check.
     * @param instructionsHandler Instruction handler to use.
     * @return true if issue is disabled at line.
     */
    private boolean isMagikIssueDisabled(
            final MagikIssue magikIssue, final LintInstructionsHandler instructionsHandler) {
        final MagikCheckHolder holder = magikIssue.check().getHolder();
        if (holder == null) {
            throw new IllegalStateException();
        }

        final Integer line = magikIssue.startLine();
        final Integer column = magikIssue.startColumn();
        if (line == null || column == null) {
            return false;
        }
        final String checkKey = holder.getCheckKeyKebabCase();

        final Map<String, String> scopeInstructions = instructionsHandler.getInstructionsInScope(line, column);
        final Map<String, String> lineInstructions = instructionsHandler.getInstructionsAtLine(line);
        final String[] scopeDisableds = scopeInstructions.getOrDefault("disable", "").split(",");
        final String[] lineDisableds = lineInstructions.getOrDefault("disable", "").split(",");
        return List.of(scopeDisableds).contains(checkKey)
            || List.of(lineDisableds).contains(checkKey);
    }

    /**
     * Run {{MagikCheckHolder}}s on {{MagikFile}}.
     * @param magikFile File to run on.
     * @param holders {{MagikCheckHolder}}s to run.
     * @return List of {{MagikIssue}}s for the given file.
     */
    private List<MagikIssue> runChecksOnFile(final MagikFile magikFile, final Iterable<MagikCheckHolder> holders) {
        LOGGER.trace("Thread: {}, checking file: {}", Thread.currentThread().getName(), magikFile);

        final LintInstructionsHandler instructionsHandler = new LintInstructionsHandler(magikFile);
        final List<MagikIssue> magikIssues = new ArrayList<>();

        // run checks on files
        for (final MagikCheckHolder holder : holders) {
            if (!holder.isEnabled()) {
                continue;
            }

            try {
                final List<MagikIssue> issues = this.runCheckOnFile(magikFile, holder).stream()
                    .filter(magikIssue -> !this.isMagikIssueDisabled(magikIssue, instructionsHandler))
                    .collect(Collectors.toList());
                magikIssues.addAll(issues);
            } catch (ReflectiveOperationException exception) {
                LOGGER.error(exception.getMessage(), exception);
            }

        }
        return magikIssues;
    }

    /**
     * Run the linter on {{paths}}.
     *
     * @throws IOException -
     * @throws ReflectiveOperationException -
     */
    public void run(final Collection<Path> paths) throws IOException, ReflectiveOperationException {
        // Gather ignore matchers.
        final List<PathMatcher> ignoreMatchers;
        if (this.config.hasProperty("ignore")) {
            @SuppressWarnings("java:S2259")
            final String[] ignores = this.config.getPropertyString("ignore").split(",");
            final FileSystem fs = FileSystems.getDefault();
            ignoreMatchers = Arrays.stream(ignores)
                .map(fs::getPathMatcher)
                .collect(Collectors.toList());
        } else {
            ignoreMatchers = new ArrayList<>();
        }

        final Long untabify;
        if (this.config.hasProperty("untabify")) {
            untabify = Long.parseLong(this.config.getPropertyString("untabify"));
        } else {
            untabify = null;
        }

        final long maxInfractions;
        if (this.config.hasProperty("max-infractions")) {
            maxInfractions = Long.parseLong(this.config.getPropertyString("max-infractions"));
        } else {
            maxInfractions = Long.MAX_VALUE;
        }

        // Sorting.
        final Location.LocationRangeComparator locationCompare = new Location.LocationRangeComparator();
        final Iterable<MagikCheckHolder> holders = MagikLint.getAllChecks(this.config);
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
            .map(path -> this.buildMagikFile(path, untabify))
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
     * Run the linter on {{magikFile}}.
     * @param magikFile File to run on.
     * @throws ReflectiveOperationException -
     */
    public void run(final MagikFile magikFile) throws ReflectiveOperationException {
        final Iterable<MagikCheckHolder> holders = MagikLint.getAllChecks(this.config);
        final Comparator<MagikIssue> byLine = Comparator.comparing(MagikIssue::startLine);
        final Comparator<MagikIssue> byColumn = Comparator.comparing(MagikIssue::startColumn);
        this.runChecksOnFile(magikFile, holders).stream()
            .sorted(byLine.thenComparing(byColumn))
            .forEach(this.reporter::reportIssue);
    }

    /**
     * Get all checks, enabled in the given configuration.
     *
     * @param config Configuration to use
     * @return Collection of {{MagikCheckHolder}}s.
     */
    @SuppressWarnings("unchecked")
    public static List<MagikCheckHolder> getAllChecks(final Configuration config) {
        final List<MagikCheckHolder> holders = new ArrayList<>();

        final String disabledProperty = config.getPropertyString("disabled");
        final String disabled = disabledProperty != null
            ? disabledProperty
            : "";
        final List<String> disableds = Arrays.stream(disabled.split(","))
            .map(String::trim)
            .collect(Collectors.toList());

        for (final Class<?> checkClass : CheckList.getChecks()) {
            final Rule annotation = checkClass.getAnnotation(Rule.class);
            final String checkKey = annotation.key();
            final String checkKeyKebabCase = MagikCheckHolder.toKebabCase(checkKey);
            final boolean enabled = !disableds.contains(checkKeyKebabCase);

            // Gather parameters from MagikCheck, value from config.
            final String name = checkClass.getAnnotation(Rule.class).key();
            final Set<MagikCheckHolder.Parameter> parameters = Arrays.stream(checkClass.getFields())
                .map(field -> field.getAnnotation(RuleProperty.class))
                .filter(Objects::nonNull)
                .filter(ruleProperty -> config.hasProperty(name + "." + ruleProperty.key().replace(" ", "-")))
                .map(ruleProperty -> {
                    final String key = ruleProperty.key().replace(" ", "-");
                    final String configKey = name + "." + key;

                    // Store parameter.
                    final String description = ruleProperty.description();
                    final MagikCheckHolder.Parameter parameter;
                    if (ruleProperty.type().equals("INTEGER")) {
                        final Integer configValue = config.getPropertyInt(configKey);
                        parameter = new MagikCheckHolder.Parameter(key, description, configValue);
                    } else if (ruleProperty.type().equals("STRING")) {
                        final String configValue = config.getPropertyString(configKey);
                        parameter = new MagikCheckHolder.Parameter(key, description, configValue);
                    } else if (ruleProperty.type().equals("BOOLEAN")) {
                        final Boolean configValue = config.getPropertyBoolean(configKey);
                        parameter = new MagikCheckHolder.Parameter(key, description, configValue);
                    } else {
                        throw new IllegalStateException("Unknown type for property: " + ruleProperty.type());
                    }
                    return parameter;
                })
                .collect(Collectors.toSet());

            final MagikCheckHolder holder = new MagikCheckHolder((Class<MagikCheck>) checkClass, parameters, enabled);
            holders.add(holder);
        }
        return holders;
    }

}
