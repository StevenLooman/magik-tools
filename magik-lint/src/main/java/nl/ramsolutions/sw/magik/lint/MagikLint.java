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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import nl.ramsolutions.sw.FileCharsetDeterminer;
import nl.ramsolutions.sw.Utils;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.analysis.Location;
import nl.ramsolutions.sw.magik.analysis.scope.GlobalScope;
import nl.ramsolutions.sw.magik.analysis.scope.Scope;
import nl.ramsolutions.sw.magik.checks.CheckList;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import nl.ramsolutions.sw.magik.checks.MagikCheckHolder;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import nl.ramsolutions.sw.magik.lint.output.Reporter;
import nl.ramsolutions.sw.magik.parser.CommentInstructionReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;

/**
 * Magik Lint main class.
 */
public class MagikLint {

    private static final Logger LOGGER = LoggerFactory.getLogger(MagikLint.class);

    private static final CommentInstructionReader.InstructionType MLINT_INSTRUCTION =
        CommentInstructionReader.InstructionType.createInstructionType("mlint");
    private static final CommentInstructionReader.InstructionType MLINT_SCOPE_INSTRUCTION =
        CommentInstructionReader.InstructionType.createScopeInstructionType("mlint");

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
     * @param path     Path to file
     * @param untabify Untabify to N-spaces, if given
     * @return Visitor context for file.
     */
    private MagikFile buildMagikFile(final Path path, final @Nullable Integer untabify) {
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
            fileContents = Utils.untabify(fileContents, untabify);
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
     * @param writer Writer Write to write output to.
     * @param showDisabled boolean Boolean to show disabled checks or not.
     * @throws ReflectiveOperationException -
     * @throws IOException -
     */
    void showChecks(final Writer writer, final boolean showDisabled) throws ReflectiveOperationException, IOException {
        Iterable<MagikCheckHolder> holders = MagikLint.getAllChecks(this.config);
        for (final MagikCheckHolder holder : holders) {
            final String name = holder.getSqKey();

            if (!showDisabled && holder.isEnabled() || showDisabled && !holder.isEnabled()) {
                writer.write("- " + name + " (" + holder.getTitle() + ")\n");
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
    private boolean isMagikIssueDisabled(
            final MagikFile magikFile,
            final MagikIssue magikIssue,
            final CommentInstructionReader instructionReader) {
        final MagikCheckHolder holder = magikIssue.check().getHolder();
        Objects.requireNonNull(holder);

        final Integer line = magikIssue.startLine();
        final String checkKey = holder.getCheckKeyKebabCase();

        final Map<String, String> scopeInstructions =
            MagikLint.getScopeInstructions(magikFile, instructionReader, line);
        final Map<String, String> lineInstructions = MagikLint.getLineInstructions(instructionReader, line);
        final String[] scopeDisableds = scopeInstructions.getOrDefault("disable", "").split(",");
        final String[] lineDisableds = lineInstructions.getOrDefault("disable", "").split(",");
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

        final CommentInstructionReader instructionReader = new CommentInstructionReader(
            magikFile, Set.of(MLINT_INSTRUCTION, MLINT_SCOPE_INSTRUCTION));
        final List<MagikIssue> magikIssues = new ArrayList<>();

        // run checks on files
        for (final MagikCheckHolder holder : holders) {
            if (!holder.isEnabled()) {
                continue;
            }

            try {
                final List<MagikIssue> issues = this.runCheckOnFile(magikFile, holder).stream()
                    .filter(magikIssue -> !this.isMagikIssueDisabled(magikFile, magikIssue, instructionReader))
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

        final Integer untabify;
        if (this.config.hasProperty("untabify")) {
            untabify = Integer.parseInt(this.config.getPropertyString("untabify"));
            if (untabify < 1) {
                throw new NumberFormatException("Must be a positive integer.");
            }
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
     * Run the linter on {@code magikFile}.
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
     * @return Collection of {@link MagikCheckHolder}s.
     */
    @SuppressWarnings("unchecked")
    public static List<MagikCheckHolder> getAllChecks(final Configuration config) {
        final List<MagikCheckHolder> holders = new ArrayList<>();

        final String disabledProperty = config.getPropertyString("disabled");
        final String disabledString = disabledProperty != null
            ? disabledProperty
            : "";
        final List<String> disableds = Arrays.stream(disabledString.split(","))
            .map(String::trim)
            .collect(Collectors.toList());

        final String enabledProperty = config.getPropertyString("enabled");
        final String enabledString = enabledProperty != null
            ? enabledProperty
            : "";
        final List<String> enableds = Arrays.stream(enabledString.split(","))
            .map(String::trim)
            .collect(Collectors.toList());
        enableds.remove("");

        for (final Class<?> checkClass : CheckList.getChecks()) {
            final Rule annotation = checkClass.getAnnotation(Rule.class);
            final String checkKey = annotation.key();
            final String checkKeyKebabCase = MagikCheckHolder.toKebabCase(checkKey);
            final boolean enabled = !enableds.isEmpty()
                ? enableds.contains(checkKeyKebabCase)
                : !disableds.contains(checkKeyKebabCase);

            // Gather parameters from MagikCheck, value from config.
            final String name = MagikCheckHolder.toKebabCase(checkClass.getAnnotation(Rule.class).key());
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

    /**
     * Get scope instructions at line.
     * @param magikFile Magik file.
     * @param instructionReader Instruction reader to use.
     * @param line Line of scope.
     * @return Instructions in scope and ancestor scopes.
     */
    public static Map<String, String> getScopeInstructions(
            final MagikFile magikFile,
            final CommentInstructionReader instructionReader,
            final int line) {
        final Map<String, String> instructions = new HashMap<>();
        final GlobalScope globalScope = magikFile.getGlobalScope();
        if (globalScope == null) {
            return instructions;
        }

        // Ensure we can find a Scope.
        final String[] sourceLines = magikFile.getSourceLines();
        final int column = sourceLines[line - 1].length() - 1;
        final Scope fromScope = globalScope.getScopeForLineColumn(line, column);
        if (fromScope == null) {
            return instructions;
        }

        // Iterate over all ancestor scopes, see if the check is disabled in any scope.
        final List<Scope> scopes = fromScope.getSelfAndAncestorScopes();
        // Reverse such that if a narrower scope overrides a broader scope instruction.
        Collections.reverse(scopes);
        for (final Scope scope : scopes) {
            final Set<String> scopeInstructions =
                instructionReader.getScopeInstructions(scope, MLINT_SCOPE_INSTRUCTION);
            final Map<String, String> parsedScopeInstructions = scopeInstructions.stream()
                .map(CommentInstructionReader::parseInstructions)
                .reduce(
                    instructions,
                    (acc, elem) -> {
                        acc.putAll(elem);
                        return acc;
                    });
            instructions.putAll(parsedScopeInstructions);
        }

        return instructions;
    }

    private static Map<String, String> getLineInstructions(
            final CommentInstructionReader instructionReader,
            final int line) {
        final String str = instructionReader.getInstructionsAtLine(line, MLINT_INSTRUCTION);
        if (str == null) {
            return Collections.emptyMap();
        }

        return CommentInstructionReader.parseInstructions(str);
    }

}
