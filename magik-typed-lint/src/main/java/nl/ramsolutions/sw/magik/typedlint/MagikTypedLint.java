package nl.ramsolutions.sw.magik.typedlint;

import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import nl.ramsolutions.sw.ConfigurationReader;
import nl.ramsolutions.sw.FileCharsetDeterminer;
import nl.ramsolutions.sw.MagikToolsProperties;
import nl.ramsolutions.sw.checks.Check;
import nl.ramsolutions.sw.checks.CheckHolder;
import nl.ramsolutions.sw.checks.CheckMetadata;
import nl.ramsolutions.sw.checks.ChecksConfiguration;
import nl.ramsolutions.sw.checks.Issue;
import nl.ramsolutions.sw.checks.IssueDisabledChecker;
import nl.ramsolutions.sw.checks.MagikTypedCheckList;
import nl.ramsolutions.sw.checks.output.Reporter;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Magik Lint main class. */
public class MagikTypedLint {

  public static final String KEY_MAX_INFRACTIONS = "magik.lint.max-infractions";
  public static final String KEY_COLUMN_OFFSET = "magik.lint.column-offset";
  public static final String KEY_MSG_TEMPLATE = "magik.lint.msg-template";
  public static final String KEY_OVERRIDE_CONFIG = "magik.lint.overrideConfigFile";

  private static final Logger LOGGER = LoggerFactory.getLogger(MagikTypedLint.class);

  private final IDefinitionKeeper definitionKeeper;
  private final MagikToolsProperties properties;
  private final Reporter reporter;

  /**
   * Constructor, parses command line and reads configuration.
   *
   * @param definitionKeeper {@link IDefinitionKeeper} to use.
   * @param properties Properties.
   * @param reporter Reporter.
   */
  public MagikTypedLint(
      final IDefinitionKeeper definitionKeeper,
      final MagikToolsProperties properties,
      final Reporter reporter) {
    this.definitionKeeper = definitionKeeper;
    this.properties = properties;
    this.reporter = reporter;
  }

  /**
   * Build context for a file.
   *
   * @param path Path to file
   * @return Visitor context for file.
   * @throws IOException -
   */
  private MagikTypedFile buildMagikFile(final Path path) {
    try {
      final MagikToolsProperties fileProperties =
          ConfigurationReader.readProperties(path, this.properties);
      final URI uri = path.toUri();
      final Charset charset = FileCharsetDeterminer.determineCharset(path);
      final String fileContents = Files.readString(path, charset);
      return new MagikTypedFile(fileProperties, uri, fileContents, this.definitionKeeper);
    } catch (final IOException exception) {
      throw new IllegalStateException(exception);
    }
  }

  /**
   * Run a single check on context.
   *
   * @param magikFile File to run check on.
   * @param holder {@link CheckHolder} Check to run.
   * @return {@link Issue}s/infractions found.
   * @throws ReflectiveOperationException -
   */
  private List<Issue> runCheckOnFile(final MagikTypedFile magikFile, final CheckHolder holder)
      throws ReflectiveOperationException {
    final Check check = holder.createCheck();
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
  void showChecks(final Writer writer, final boolean showDisabled)
      throws ReflectiveOperationException, IOException {
    final List<Class<? extends Check>> checks = MagikTypedCheckList.INSTANCE.getBaseChecks();
    final ChecksConfiguration checksConfig = new ChecksConfiguration(checks, this.properties);
    final Iterable<CheckHolder> holders = checksConfig.getAllChecks();
    for (final CheckHolder holder : holders) {
      final CheckMetadata metadata = holder.getMetadata();
      if (!showDisabled && holder.isEnabled() || showDisabled && !holder.isEnabled()) {
        writer.write("- " + metadata.getSqKey() + " (" + metadata.getTitle() + ")\n");
      } else {
        continue;
      }

      for (final CheckHolder.Parameter parameter : holder.getParameters()) {
        writer.write(
            " ".repeat(2)
                + "*"
                + " "
                + parameter.getName()
                + ":"
                + " "
                + parameter.getValue()
                + " "
                + "("
                + parameter.getDescription()
                + ")\n");
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
   * Run the linter on {@code paths}.
   *
   * @throws IOException -
   * @throws ReflectiveOperationException -
   */
  public void run(final Collection<Path> paths) throws IOException, ReflectiveOperationException {
    final long maxInfractions =
        this.properties.getPropertyLong(MagikTypedLint.KEY_MAX_INFRACTIONS, Long.MAX_VALUE);
    final Location.LocationRangeComparator locationCompare = new Location.LocationRangeComparator();
    paths.stream()
        .parallel()
        .map(this::buildMagikFile)
        .filter(magikFile -> !this.isFileIgnored(magikFile))
        .map(this::runChecksOnFile)
        .flatMap(List::stream)
        .sorted((issue0, issue1) -> locationCompare.compare(issue0.location(), issue1.location()))
        .sequential()
        .limit(maxInfractions)
        .forEach(this.reporter::reportIssue);
  }

  private boolean isFileIgnored(final MagikFile magikFile) {
    // TODO: Is this still current?
    final MagikToolsProperties fileProperties = magikFile.getProperties();
    final List<Class<? extends Check>> checks = MagikTypedCheckList.INSTANCE.getBaseChecks();
    final ChecksConfiguration checksConfig = new ChecksConfiguration(checks, fileProperties);
    final URI uri = magikFile.getUri();
    final Path path = Path.of(uri);
    final FileSystem fs = FileSystems.getDefault();
    final boolean isIgnored =
        checksConfig.getIgnores().stream()
            .map(fs::getPathMatcher)
            .anyMatch(matcher -> matcher.matches(path));
    if (isIgnored) {
      LOGGER.trace("Thread: {}, ignoring file: {}", Thread.currentThread().getName(), path);
    }
    return isIgnored;
  }

  /**
   * Run {@link CheckHolder}s on {@link MagikTypedFile}.
   *
   * @param magikFile File to run on.
   * @param holders {@link CheckHolder}s to run.
   * @return List of {@link Issue}s for the given file.
   */
  private List<Issue> runChecksOnFile(final MagikTypedFile magikFile) {
    LOGGER.trace("Thread: {}, checking file: {}", Thread.currentThread().getName(), magikFile);

    final List<Issue> allIssues = new ArrayList<>();

    // Run checks on files.
    final MagikToolsProperties fileProperties = magikFile.getProperties();
    final List<Class<? extends Check>> checks = MagikTypedCheckList.INSTANCE.getBaseChecks();
    final ChecksConfiguration checksConfig = new ChecksConfiguration(checks, fileProperties);
    final Iterable<CheckHolder> holders = checksConfig.getAllChecks();
    for (final CheckHolder holder : holders) {
      if (!holder.isEnabled()) {
        continue;
      }

      try {
        final List<Issue> issues =
            this.runCheckOnFile(magikFile, holder).stream()
                .filter(issue -> !IssueDisabledChecker.issueDisabled(magikFile, issue))
                .toList();
        allIssues.addAll(issues);
      } catch (final ReflectiveOperationException exception) {
        LOGGER.error(exception.getMessage(), exception);
      }
    }

    return allIssues;
  }
}
