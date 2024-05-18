package nl.ramsolutions.sw.magik.lint;

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
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.checks.CheckList;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import nl.ramsolutions.sw.magik.checks.MagikCheckHolder;
import nl.ramsolutions.sw.magik.checks.MagikCheckMetadata;
import nl.ramsolutions.sw.magik.checks.MagikChecksConfiguration;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import nl.ramsolutions.sw.magik.checks.MagikIssueDisabledChecker;
import nl.ramsolutions.sw.magik.lint.output.Reporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Magik Lint main class. */
public class MagikLint {

  public static final String KEY_MAX_INFRACTIONS = "magik.lint.max-infractions";
  public static final String KEY_COLUMN_OFFSET = "magik.lint.column-offset";
  public static final String KEY_MSG_TEMPLATE = "magik.lint.msg-template";
  public static final String KEY_OVERRIDE_CONFIG = "magik.lint.overrideConfigFile";

  private static final Logger LOGGER = LoggerFactory.getLogger(MagikLint.class);

  private final MagikToolsProperties properties;
  private final Reporter reporter;

  /**
   * Constructor, parses command line and reads configuration.
   *
   * @param configuration Configuration.
   * @param reporter Reporter.
   */
  public MagikLint(final MagikToolsProperties properties, final Reporter reporter) {
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
  private MagikFile buildMagikFile(final Path path) {
    try {
      final MagikToolsProperties fileProperties =
          ConfigurationReader.readProperties(path, this.properties);
      final URI uri = path.toUri();
      final Charset charset = FileCharsetDeterminer.determineCharset(path);
      final String fileContents = Files.readString(path, charset);
      return new MagikFile(fileProperties, uri, fileContents);
    } catch (final IOException exception) {
      LOGGER.error("Caught exception:", exception);
      throw new IllegalStateException(exception);
    }
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
  void showChecks(final Writer writer, final boolean showDisabled)
      throws ReflectiveOperationException, IOException {
    final Path overrideConfigPath = this.properties.getPropertyPath(MagikLint.KEY_OVERRIDE_CONFIG);
    final MagikToolsProperties properties =
        overrideConfigPath != null
            ? new MagikToolsProperties(overrideConfigPath)
            : MagikToolsProperties.DEFAULT_PROPERTIES;
    final MagikChecksConfiguration checksConfig =
        new MagikChecksConfiguration(CheckList.getChecks(), properties);
    final Iterable<MagikCheckHolder> holders = checksConfig.getAllChecks();
    for (final MagikCheckHolder holder : holders) {
      final MagikCheckMetadata metadata = holder.getMetadata();
      if (!showDisabled && holder.isEnabled() || showDisabled && !holder.isEnabled()) {
        writer.write("- " + metadata.getSqKey() + " (" + metadata.getTitle() + ")\n");
      } else {
        continue;
      }

      for (final MagikCheckHolder.Parameter parameter : holder.getParameters()) {
        writer.write(
            "\t"
                + parameter.getName()
                + ":\t"
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
    final long maxInfractions = this.properties.getPropertyLong(MagikLint.KEY_MAX_INFRACTIONS);
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
    final MagikToolsProperties fileProperties = magikFile.getProperties();
    final MagikChecksConfiguration checksConfig =
        new MagikChecksConfiguration(CheckList.getChecks(), fileProperties);
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
   * Run {@link MagikCheckHolder}s on {@link MagikFile}.
   *
   * @param magikFile File to run on.
   * @param holders {@link MagikCheckHolder}s to run.
   * @return List of {@link MagikIssue}s for the given file.
   */
  private List<MagikIssue> runChecksOnFile(final MagikFile magikFile) {
    LOGGER.trace("Thread: {}, checking file: {}", Thread.currentThread().getName(), magikFile);

    final List<MagikIssue> magikIssues = new ArrayList<>();

    // Run checks on files.
    final MagikToolsProperties fileProperties = magikFile.getProperties();
    final MagikChecksConfiguration checksConfig =
        new MagikChecksConfiguration(CheckList.getChecks(), fileProperties);
    final Iterable<MagikCheckHolder> holders = checksConfig.getAllChecks();
    for (final MagikCheckHolder holder : holders) {
      if (!holder.isEnabled()) {
        continue;
      }

      try {
        final List<MagikIssue> issues =
            this.runCheckOnFile(magikFile, holder).stream()
                .filter(
                    magikIssue -> !MagikIssueDisabledChecker.issueDisabled(magikFile, magikIssue))
                .toList();
        magikIssues.addAll(issues);
      } catch (final ReflectiveOperationException exception) {
        LOGGER.error(exception.getMessage(), exception);
      }
    }

    return magikIssues;
  }
}
