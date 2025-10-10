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
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import nl.ramsolutions.sw.ConfigurationReader;
import nl.ramsolutions.sw.FileCharsetDeterminer;
import nl.ramsolutions.sw.MagikToolsProperties;
import nl.ramsolutions.sw.OpenedFile;
import nl.ramsolutions.sw.SourceFileScanner;
import nl.ramsolutions.sw.checks.Check;
import nl.ramsolutions.sw.checks.CheckHolder;
import nl.ramsolutions.sw.checks.CheckMetadata;
import nl.ramsolutions.sw.checks.ChecksConfiguration;
import nl.ramsolutions.sw.checks.Issue;
import nl.ramsolutions.sw.checks.IssueDisabledChecker;
import nl.ramsolutions.sw.checks.MagikCheckList;
import nl.ramsolutions.sw.checks.ProductDefCheckList;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.lint.output.Reporter;
import nl.ramsolutions.sw.productdef.ProductDefFile;
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
   * @param properties Properties.
   * @param reporter Reporter.
   */
  public MagikLint(final MagikToolsProperties properties, final Reporter reporter) {
    this.properties = properties;
    this.reporter = reporter;
  }

  /**
   * Build {@link OpenedFile} for path.
   *
   * @param path Path to file
   * @return {@link OpenedFile} for path.
   * @throws IOException -
   */
  private OpenedFile buildOpenedFile(final Path path) {
    try {
      final MagikToolsProperties fileProperties =
          ConfigurationReader.readProperties(path, this.properties);
      final URI uri = path.toUri();
      final Charset charset = FileCharsetDeterminer.determineCharset(path);
      final String fileContents = Files.readString(path, charset);

      if (SourceFileScanner.MAGIK_FILE_FILTER.test(path)) {
        return new MagikFile(fileProperties, uri, fileContents);
      } else if (SourceFileScanner.PRODUCT_DEF_FILE_FILTER.test(path)) {
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper(false);
        return new ProductDefFile(fileProperties, uri, fileContents, definitionKeeper, null);
      } else {
        throw new IllegalStateException("Unsupported file type: " + path);
      }
    } catch (final IOException exception) {
      throw new IllegalStateException(exception);
    }
  }

  /**
   * Run a single check on context.
   *
   * @param openedFile File to run check on.
   * @param holder {@link CheckHolder} {@link Check} to run.
   * @return Issues/infractions found.
   * @throws ReflectiveOperationException -
   */
  private List<Issue> runCheckOnFile(final OpenedFile openedFile, final CheckHolder holder)
      throws ReflectiveOperationException {
    final Check check = holder.createCheck();
    return check.scanFileForIssues(openedFile);
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
    final List<Class<? extends Check>> checks = this.getAllChecks();
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
        this.properties.getPropertyLong(MagikLint.KEY_MAX_INFRACTIONS, Long.MAX_VALUE);
    final Location.LocationRangeComparator locationCompare = new Location.LocationRangeComparator();
    paths.stream()
        .parallel()
        .map(this::buildOpenedFile)
        .filter(openedFile -> !this.isFileIgnored(openedFile))
        .map(this::runChecksOnFile)
        .flatMap(List::stream)
        .sorted((issue0, issue1) -> locationCompare.compare(issue0.location(), issue1.location()))
        .sequential()
        .limit(maxInfractions)
        .forEach(this.reporter::reportIssue);
  }

  private boolean isFileIgnored(final OpenedFile openedFile) {
    final MagikToolsProperties fileProperties = openedFile.getProperties();
    final List<Class<? extends Check>> checks = this.getChecksForOpenedFile(openedFile);
    final ChecksConfiguration checksConfig = new ChecksConfiguration(checks, fileProperties);
    final URI uri = openedFile.getUri();
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
   * Run {@link CheckHolder}s on {@link OpenedFile}.
   *
   * @param openedFile File to run on.
   * @param holders {@link CheckHolder}s to run.
   * @return List of {@link Issue}s for the given file.
   */
  private List<Issue> runChecksOnFile(final OpenedFile openedFile) {
    LOGGER.trace("Thread: {}, checking file: {}", Thread.currentThread().getName(), openedFile);

    final List<Issue> allIssues = new ArrayList<>();

    // Run checks on files.
    final MagikToolsProperties fileProperties = openedFile.getProperties();
    final List<Class<? extends Check>> checks = this.getChecksForOpenedFile(openedFile);
    final ChecksConfiguration checksConfig = new ChecksConfiguration(checks, fileProperties);
    final Iterable<CheckHolder> holders = checksConfig.getAllChecks();
    for (final CheckHolder holder : holders) {
      if (!holder.isEnabled()) {
        continue;
      }

      try {
        final List<Issue> issues =
            this.runCheckOnFile(openedFile, holder).stream()
                .filter(magikIssue -> !IssueDisabledChecker.issueDisabled(openedFile, magikIssue))
                .toList();
        allIssues.addAll(issues);
      } catch (final ReflectiveOperationException exception) {
        LOGGER.error(exception.getMessage(), exception);
      }
    }

    return allIssues;
  }

  private List<Class<? extends Check>> getChecksForOpenedFile(final OpenedFile openedFile) {
    if (openedFile instanceof MagikFile) {
      return MagikCheckList.getBaseChecks();
    } else if (openedFile instanceof ProductDefFile) {
      return ProductDefCheckList.getBaseChecks();
    } else {
      throw new IllegalStateException("Unsupported file type: " + openedFile.getClass());
    }
  }

  private List<Class<? extends Check>> getAllChecks() {
    return Stream.concat(
            ProductDefCheckList.getBaseChecks().stream(), MagikCheckList.getBaseChecks().stream())
        .sorted(Comparator.comparing(Class::getSimpleName))
        .toList();
  }
}
