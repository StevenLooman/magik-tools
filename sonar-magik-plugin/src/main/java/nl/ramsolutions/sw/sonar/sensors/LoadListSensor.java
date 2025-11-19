package nl.ramsolutions.sw.sonar.sensors;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import nl.ramsolutions.sw.checks.Issue;
import nl.ramsolutions.sw.checks.LoadListCheck;
import nl.ramsolutions.sw.checks.LoadListCheckList;
import nl.ramsolutions.sw.loadlist.LoadListFile;
import nl.ramsolutions.sw.loadlist.metrics.FileMetrics;
import nl.ramsolutions.sw.sonar.LoadListRulesDefinition;
import nl.ramsolutions.sw.sonar.language.LoadListLanguage;
import nl.ramsolutions.sw.sonar.sensors.cpd.CpdTokenSaver;
import nl.ramsolutions.sw.sonar.visitors.LoadListHighlighterVisitor;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.rule.Checks;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.issue.NoSonarFilter;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.api.measures.Metric;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.squidbridge.ProgressReport;

/** load_list.txt/patch_list.txt squid Sensor. */
public class LoadListSensor implements Sensor {

  private static final Logger LOGGER = Loggers.get(LoadListSensor.class);
  private static final long SLEEP_PERIOD = 100;

  private final CheckFactory checkFactory;
  private final FileLinesContextFactory fileLinesContextFactory;
  private final NoSonarFilter noSonarFilter;

  /**
   * Constructor.
   *
   * @param checkFactory Factory.
   */
  public LoadListSensor(
      final CheckFactory checkFactory,
      final FileLinesContextFactory fileLinesContextFactory,
      final NoSonarFilter noSonarFilter) {
    this.checkFactory = checkFactory;
    this.fileLinesContextFactory = fileLinesContextFactory;
    this.noSonarFilter = noSonarFilter;
  }

  @Override
  public void describe(final @NonNull SensorDescriptor descriptor) {
    descriptor.onlyOnLanguage(LoadListLanguage.KEY).name("LoadList Sensor");
  }

  @Override
  public void execute(final @NonNull SensorContext context) {
    LOGGER.debug("Executing LoadListSensor");

    final FileSystem fileSystem = context.fileSystem();
    final FilePredicates predicates = fileSystem.predicates();

    final FilePredicate filePredicate =
        predicates.and(
            predicates.hasType(InputFile.Type.MAIN),
            predicates.hasLanguage(LoadListLanguage.KEY),
            predicates.or(
                predicates.matchesPathPattern("**/load_list.txt"),
                predicates.matchesPathPattern("**/patch_list.txt")));

    final List<InputFile> inputFiles = new ArrayList<>();
    fileSystem.inputFiles(filePredicate).forEach(inputFiles::add);

    final ProgressReport progressReport =
        new ProgressReport(
            "Report about progress of Sonar load_list/patch_list analyzer", SLEEP_PERIOD);
    final List<String> filenames = inputFiles.stream().map(InputFile::toString).toList();
    progressReport.start(filenames);

    for (final InputFile inputFile : inputFiles) {
      this.scanLoadListFile(context, inputFile);
      progressReport.nextFile();
    }

    progressReport.stop();
  }

  private void scanLoadListFile(final SensorContext context, final InputFile inputFile) {
    LOGGER.debug("Scanning load_list/patch_list file: {}", inputFile);

    // Read contents.
    final URI uri = inputFile.uri();
    final String fileContent;
    try {
      fileContent = inputFile.contents();
    } catch (IOException ex) {
      throw new IllegalStateException("Cannot read " + inputFile, ex);
    }

    final LoadListFile loadListFile = new LoadListFile(uri, fileContent);

    // Save metrics.
    LOGGER.debug("Save measures");
    this.saveMetrics(context, inputFile, loadListFile);

    // Save issues.
    LOGGER.debug("Running checks");
    final Checks<LoadListCheck> checks =
        checkFactory
            .<LoadListCheck>create(LoadListRulesDefinition.REPOSITORY_KEY)
            .addAnnotatedChecks(LoadListCheckList.INSTANCE.getChecks());
    for (final LoadListCheck check : checks.all()) {
      LOGGER.debug("Running check: {}", check);
      final List<Issue> issues = check.scanFileForIssues(loadListFile);
      final RuleKey ruleKey = checks.ruleKey(check);
      if (ruleKey == null) {
        continue;
      }

      this.saveIssues(context, ruleKey, issues, inputFile);
    }

    // Save highlighted tokens.
    LOGGER.debug("Saving highlighted tokens");
    final LoadListHighlighterVisitor tokensVisitor =
        new LoadListHighlighterVisitor(context, inputFile);
    tokensVisitor.scanFile(loadListFile);

    // Save CPD tokens.
    LOGGER.debug("Saving CPD tokens");
    final CpdTokenSaver cpdTokenSaver = new CpdTokenSaver(context);
    cpdTokenSaver.saveCpdTokens(inputFile, loadListFile);
  }

  private void saveMetrics(
      final SensorContext context, final InputFile inputFile, final LoadListFile loadListFile) {
    final FileMetrics metrics = new FileMetrics(loadListFile, true);

    // Metrics on file.
    this.saveMetric(context, inputFile, CoreMetrics.NCLOC, metrics.linesOfEntries().size());
    this.saveMetric(context, inputFile, CoreMetrics.COMMENT_LINES, metrics.commentLineCount());

    // Metrics on lines.
    final FileLinesContext fileLinesContext = this.fileLinesContextFactory.createFor(inputFile);
    metrics
        .linesOfEntries()
        .forEach(line -> fileLinesContext.setIntValue(CoreMetrics.NCLOC_DATA_KEY, line, 1));
    fileLinesContext.save();

    // No sonar filter.
    this.noSonarFilter.noSonarInFile(inputFile, metrics.nosonarLines());
  }

  private void saveMetric(
      final SensorContext context,
      final InputFile inputFile,
      final Metric<Integer> metric,
      final Integer value) {
    LOGGER.debug("Saving metric, file: {}, metric: {} value: {}", inputFile, metric, value);

    context.<Integer>newMeasure().withValue(value).forMetric(metric).on(inputFile).save();
  }

  private void saveIssues(
      final SensorContext context,
      final RuleKey ruleKey,
      final List<Issue> issues,
      final InputFile inputFile) {
    for (final Issue loadListIssue : issues) {
      LOGGER.debug("Saving issue, file: {}, issue: {}", inputFile, loadListIssue);

      final NewIssue issue = context.newIssue();
      final NewIssueLocation location =
          issue.newLocation().on(inputFile).message(loadListIssue.message());
      final Integer line = loadListIssue.startLine();
      if (line != null) {
        location.at(inputFile.selectLine(line));
      }
      issue.at(location).forRule(ruleKey).save();
    }
  }
}
