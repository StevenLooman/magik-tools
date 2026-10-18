package nl.ramsolutions.sw.sonar.sensors;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import nl.ramsolutions.sw.checks.Issue;
import nl.ramsolutions.sw.checks.MessageCheck;
import nl.ramsolutions.sw.checks.MessageCheckList;
import nl.ramsolutions.sw.message.MessageFile;
import nl.ramsolutions.sw.message.metrics.FileMetrics;
import nl.ramsolutions.sw.sonar.MessageRulesDefinition;
import nl.ramsolutions.sw.sonar.language.MessageLanguage;
import nl.ramsolutions.sw.sonar.visitors.MessageHighlighterVisitor;
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

/** message (.msg) file squid Sensor. */
public class MessageSensor implements Sensor {

  private static final Logger LOGGER = Loggers.get(MessageSensor.class);
  private static final long SLEEP_PERIOD = 100;

  private final CheckFactory checkFactory;
  private final FileLinesContextFactory fileLinesContextFactory;
  private final NoSonarFilter noSonarFilter;

  /**
   * Constructor.
   *
   * @param checkFactory Factory.
   */
  public MessageSensor(
      final CheckFactory checkFactory,
      final FileLinesContextFactory fileLinesContextFactory,
      final NoSonarFilter noSonarFilter) {
    this.checkFactory = checkFactory;
    this.fileLinesContextFactory = fileLinesContextFactory;
    this.noSonarFilter = noSonarFilter;
  }

  @Override
  public void describe(final @NonNull SensorDescriptor descriptor) {
    descriptor.onlyOnLanguage(MessageLanguage.KEY).name("Message Sensor");
  }

  @Override
  public void execute(final @NonNull SensorContext context) {
    LOGGER.debug("Executing MessageSensor");

    final FileSystem fileSystem = context.fileSystem();
    final FilePredicates predicates = fileSystem.predicates();

    final FilePredicate filePredicate =
        predicates.and(
            predicates.hasType(InputFile.Type.MAIN), predicates.hasLanguage(MessageLanguage.KEY));

    final List<InputFile> inputFiles = new ArrayList<>();
    fileSystem.inputFiles(filePredicate).forEach(inputFiles::add);

    final ProgressReport progressReport =
        new ProgressReport("Report about progress of Sonar message (.msg) analyzer", SLEEP_PERIOD);
    final List<String> filenames = inputFiles.stream().map(InputFile::toString).toList();
    progressReport.start(filenames);

    for (final InputFile inputFile : inputFiles) {
      this.scanMessageFile(context, inputFile);
      progressReport.nextFile();
    }

    progressReport.stop();
  }

  private void scanMessageFile(final SensorContext context, final InputFile inputFile) {
    LOGGER.debug("Scanning message (.msg) file: {}", inputFile);

    // Read contents.
    final URI uri = inputFile.uri();
    final String fileContent;
    try {
      fileContent = inputFile.contents();
    } catch (final IOException ex) {
      throw new IllegalStateException("Cannot read " + inputFile, ex);
    }

    final MessageFile messageFile = new MessageFile(uri, fileContent);

    // Save metrics.
    LOGGER.debug("Save measures");
    this.saveMetrics(context, inputFile, messageFile);

    // Save issues.
    LOGGER.debug("Running checks");
    final Checks<MessageCheck> checks =
        this.checkFactory
            .<MessageCheck>create(MessageRulesDefinition.REPOSITORY_KEY)
            .addAnnotatedChecks(MessageCheckList.INSTANCE.getChecks());
    for (final MessageCheck check : checks.all()) {
      LOGGER.debug("Running check: {}", check);
      final List<Issue> issues = check.scanFileForIssues(messageFile);
      final RuleKey ruleKey = checks.ruleKey(check);
      if (ruleKey == null) {
        continue;
      }

      this.saveIssues(context, ruleKey, issues, inputFile);
    }

    // Save highlighted tokens.
    LOGGER.debug("Saving highlighted tokens");
    final MessageHighlighterVisitor tokensVisitor =
        new MessageHighlighterVisitor(context, inputFile);
    tokensVisitor.scanFile(messageFile);
  }

  private void saveMetrics(
      final SensorContext context, final InputFile inputFile, final MessageFile messageFile) {
    final FileMetrics metrics = new FileMetrics(messageFile, true);

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
    for (final Issue messageIssue : issues) {
      LOGGER.debug("Saving issue, file: {}, issue: {}", inputFile, messageIssue);

      final NewIssue issue = context.newIssue();
      final NewIssueLocation location =
          issue.newLocation().on(inputFile).message(messageIssue.message());
      final Integer line = messageIssue.startLine();
      if (line != null) {
        location.at(inputFile.selectLine(line));
      }
      issue.at(location).forRule(ruleKey).save();
    }
  }
}
