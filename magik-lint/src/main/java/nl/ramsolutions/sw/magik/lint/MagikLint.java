package nl.ramsolutions.sw.magik.lint;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import nl.ramsolutions.sw.MagikLintSettings;
import nl.ramsolutions.sw.MagikToolsProperties;
import nl.ramsolutions.sw.OpenedFile;
import nl.ramsolutions.sw.checks.Check;
import nl.ramsolutions.sw.checks.CheckHolder;
import nl.ramsolutions.sw.checks.CheckMetadata;
import nl.ramsolutions.sw.checks.ChecksConfiguration;
import nl.ramsolutions.sw.checks.Issue;
import nl.ramsolutions.sw.checks.IssueDisabledChecker;
import nl.ramsolutions.sw.checks.LoadListCheckList;
import nl.ramsolutions.sw.checks.MagikCheckList;
import nl.ramsolutions.sw.checks.ModuleDefCheckList;
import nl.ramsolutions.sw.checks.ProductDefCheckList;
import nl.ramsolutions.sw.checks.output.Reporter;
import nl.ramsolutions.sw.magik.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Magik Lint main class. */
public class MagikLint {

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
    final List<Class<? extends Check>> checks = this.getAllCheckClasses();
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
    final long maxInfractions = new MagikLintSettings(this.properties).getMaxInfractions();
    final Location.LocationRangeComparator locationCompare = new Location.LocationRangeComparator();
    paths.stream()
        .parallel()
        .map(path -> Utils.buildOpenedFile(path, this.properties))
        .filter(openedFile -> !ChecksConfiguration.isFileIgnored(openedFile))
        .map(this::runChecksOnFile)
        .flatMap(List::stream)
        .sorted((issue0, issue1) -> locationCompare.compare(issue0.location(), issue1.location()))
        .sequential()
        .limit(maxInfractions)
        .forEach(this.reporter::reportIssue);
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
    return Utils.getCheckListForOpenedFile(openedFile).getBaseChecks();
  }

  private List<Class<? extends Check>> getAllCheckClasses() {
    return Stream.of(
            ProductDefCheckList.INSTANCE.getBaseChecks().stream(),
            ModuleDefCheckList.INSTANCE.getBaseChecks().stream(),
            LoadListCheckList.INSTANCE.getBaseChecks().stream(),
            MagikCheckList.INSTANCE.getBaseChecks().stream())
        .flatMap(stream -> stream)
        .sorted(Comparator.comparing(Class::getSimpleName))
        .toList();
  }
}
