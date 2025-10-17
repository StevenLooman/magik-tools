package nl.ramsolutions.sw.sonar.sensors;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import nl.ramsolutions.sw.checks.Issue;
import nl.ramsolutions.sw.checks.ModuleDefCheck;
import nl.ramsolutions.sw.checks.ModuleDefCheckList;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.moduledef.ModuleDefFile;
import nl.ramsolutions.sw.moduledef.metrics.FileMetrics;
import nl.ramsolutions.sw.sonar.ProductModuleDefRulesDefinition;
import nl.ramsolutions.sw.sonar.language.ProductModuleDefLanguage;
import nl.ramsolutions.sw.sonar.sensors.cpd.CpdTokenSaver;
import nl.ramsolutions.sw.sonar.visitors.ModuleDefHighlighterVisitor;
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

/** module.def squid Sensor. */
public class ModuleDefSensor implements Sensor {

  private static final String MODULE_DEF = "module.def";
  private static final Logger LOGGER = Loggers.get(ModuleDefSensor.class);
  private static final long SLEEP_PERIOD = 100;

  private final CheckFactory checkFactory;
  private final FileLinesContextFactory fileLinesContextFactory;
  private final NoSonarFilter noSonarFilter;

  /**
   * Constructor.
   *
   * @param checkFactory Factory.
   * @param fileLinesContextFactory Factory.
   */
  public ModuleDefSensor(
      final CheckFactory checkFactory,
      final FileLinesContextFactory fileLinesContextFactory,
      final NoSonarFilter noSonarFilter) {
    this.checkFactory = checkFactory;
    this.fileLinesContextFactory = fileLinesContextFactory;
    this.noSonarFilter = noSonarFilter;
  }

  @Override
  public void describe(final @Nonnull SensorDescriptor descriptor) {
    descriptor.onlyOnLanguage(ProductModuleDefLanguage.KEY).name("ModuleDef Sensor");
  }

  @Override
  public void execute(final @Nonnull SensorContext context) {
    LOGGER.debug("Executing ModuleDefSensor");

    final FileSystem fileSystem = context.fileSystem();
    final FilePredicates predicates = fileSystem.predicates();

    final FilePredicate filePredicate =
        predicates.and(
            predicates.hasType(InputFile.Type.MAIN),
            predicates.hasLanguage(ProductModuleDefLanguage.KEY),
            predicates.hasFilename(MODULE_DEF));

    final List<InputFile> inputFiles = new ArrayList<>();
    fileSystem.inputFiles(filePredicate).forEach(inputFiles::add);

    final ProgressReport progressReport =
        new ProgressReport("Report about progress of Sonar module.def analyzer", SLEEP_PERIOD);
    final List<String> filenames = inputFiles.stream().map(InputFile::toString).toList();
    progressReport.start(filenames);

    for (final InputFile inputFile : inputFiles) {
      this.scanModuleDefFile(context, inputFile);
      progressReport.nextFile();
    }

    progressReport.stop();
  }

  private void scanModuleDefFile(final SensorContext context, final InputFile inputFile) {
    LOGGER.debug("Scanning module.def file: {}", inputFile);

    // Read contents.
    final URI uri = inputFile.uri();
    final String fileContent;
    try {
      fileContent = inputFile.contents();
    } catch (IOException ex) {
      throw new IllegalStateException("Cannot read " + inputFile, ex);
    }

    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper(false);
    final ModuleDefFile moduleDefFile = new ModuleDefFile(uri, fileContent, definitionKeeper, null);

    // Save metrics.
    LOGGER.debug("Save measures");
    this.saveMetrics(context, inputFile, moduleDefFile);

    // Save issues.
    LOGGER.debug("Running checks");
    final Checks<ModuleDefCheck> checks =
        checkFactory
            .<ModuleDefCheck>create(ProductModuleDefRulesDefinition.REPOSITORY_KEY)
            .addAnnotatedChecks(ModuleDefCheckList.getChecks());
    for (final ModuleDefCheck check : checks.all()) {
      LOGGER.debug("Running check: {}", check);
      final List<Issue> issues = check.scanFileForIssues(moduleDefFile);
      final RuleKey ruleKey = checks.ruleKey(check);
      if (ruleKey == null) {
        continue;
      }

      this.saveIssues(context, ruleKey, issues, inputFile);
    }

    // Save highlighted tokens.
    LOGGER.debug("Saving highlighted tokens");
    final ModuleDefHighlighterVisitor tokensVisitor =
        new ModuleDefHighlighterVisitor(context, inputFile);
    tokensVisitor.scanFile(moduleDefFile);

    // Save CPD tokens.
    LOGGER.debug("Saving CPD tokens");
    final CpdTokenSaver cpdTokenSaver = new CpdTokenSaver(context);
    cpdTokenSaver.saveCpdTokens(inputFile, moduleDefFile);
  }

  private void saveMetrics(
      final SensorContext context, final InputFile inputFile, final ModuleDefFile moduleDefFile) {
    final FileMetrics metrics = new FileMetrics(moduleDefFile, true);

    // Metrics on file.
    this.saveMetric(context, inputFile, CoreMetrics.NCLOC, metrics.linesOfDefinition().size());
    this.saveMetric(context, inputFile, CoreMetrics.COMMENT_LINES, metrics.commentLineCount());
    // A module.def file is always one class.
    // TODO: Do we really want this?
    this.saveMetric(context, inputFile, CoreMetrics.CLASSES, 1);

    // Metrics on lines.
    final FileLinesContext fileLinesContext = this.fileLinesContextFactory.createFor(inputFile);
    metrics
        .linesOfDefinition()
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
    for (final Issue moduleDefIssue : issues) {
      LOGGER.debug("Saving issue, file: {}, issue: {}", inputFile, moduleDefIssue);

      final NewIssue issue = context.newIssue();
      final NewIssueLocation location =
          issue.newLocation().on(inputFile).message(moduleDefIssue.message());
      final Integer line = moduleDefIssue.startLine();
      if (line != null) {
        location.at(inputFile.selectLine(line));
      }
      issue.at(location).forRule(ruleKey).save();
    }
  }
}
