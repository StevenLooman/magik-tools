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
import nl.ramsolutions.sw.ConfigurationLocator;
import nl.ramsolutions.sw.FileCharsetDeterminer;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.analysis.MagikAnalysisConfiguration;
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

  private static final Logger LOGGER = LoggerFactory.getLogger(MagikLint.class);

  private final MagikLintConfiguration config;
  private final Reporter reporter;

  /**
   * Constructor, parses command line and reads configuration.
   *
   * @param configuration Configuration.
   * @param reporter Reporter.
   */
  public MagikLint(final MagikLintConfiguration configuration, final Reporter reporter) {
    this.config = configuration;
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
    final Charset charset = FileCharsetDeterminer.determineCharset(path);

    byte[] encoded = null;
    try {
      encoded = Files.readAllBytes(path);
    } catch (final IOException exception) {
      LOGGER.error(exception.getMessage(), exception);
    }

    final URI uri = path.toUri();
    final String fileContents = new String(encoded, charset);
    final MagikAnalysisConfiguration configuration;
    try {
      configuration = new MagikAnalysisConfiguration();
    } catch (final IOException exception) {
      throw new IllegalStateException(exception);
    }

    return new MagikFile(configuration, uri, fileContents);
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
    final Path configPath = this.config.getPath();
    final MagikChecksConfiguration checksConfig =
        configPath != null
            ? new MagikChecksConfiguration(CheckList.getChecks(), configPath)
            : new MagikChecksConfiguration(CheckList.getChecks());
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
    final long maxInfractions = this.config.getMaxInfractions();
    final Location.LocationRangeComparator locationCompare = new Location.LocationRangeComparator();
    paths.stream()
        .parallel()
        .filter(path -> !this.isFileIgnored(path))
        .map(this::buildMagikFile)
        .map(this::runChecksOnFile)
        .flatMap(List::stream)
        .sorted((issue0, issue1) -> locationCompare.compare(issue0.location(), issue1.location()))
        .limit(maxInfractions)
        .forEach(this.reporter::reportIssue);
  }

  private MagikChecksConfiguration getChecksConfig(final Path path) {
    final Path configPath =
        this.config.getPath() != null
            ? this.config.getPath()
            : ConfigurationLocator.locateConfiguration(path);
    try {
      return configPath != null
          ? new MagikChecksConfiguration(CheckList.getChecks(), configPath)
          : new MagikChecksConfiguration(CheckList.getChecks());
    } catch (final IOException exception) {
      throw new IllegalStateException(exception);
    }
  }

  private boolean isFileIgnored(final Path path) {
    final MagikChecksConfiguration checksConfig = this.getChecksConfig(path);
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

    // run checks on files
    final URI uri = magikFile.getUri();
    final Path path = Path.of(uri);
    final MagikChecksConfiguration checksConfig = this.getChecksConfig(path);
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
