package org.stevenlooman.sw.sonar.sensors;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.RecognitionException;
import com.sonar.sslr.api.Token;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputFile.Type;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.rule.Checks;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.cpd.NewCpdTokens;
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
import org.stevenlooman.sw.magik.MagikIssue;
import org.stevenlooman.sw.magik.MagikCheck;
import org.stevenlooman.sw.magik.MagikVisitor;
import org.stevenlooman.sw.magik.MagikVisitorContext;
import org.stevenlooman.sw.magik.CheckList;
import org.stevenlooman.sw.magik.metrics.FileMetrics;
import org.stevenlooman.sw.magik.parser.FileCharsetDeterminer;
import org.stevenlooman.sw.magik.parser.MagikParser;
import org.stevenlooman.sw.sonar.TokenLocation;
import org.stevenlooman.sw.sonar.language.Magik;
import org.stevenlooman.sw.sonar.visitors.MagikHighlighterVisitor;

import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MagikSquidSensor implements Sensor {

  private static final Logger LOGGER = Loggers.get(MagikSquidSensor.class);
  private final Checks<MagikCheck> checks;
  private final FileLinesContextFactory fileLinesContextFactory;
  private final NoSonarFilter noSonarFilter;

  /**
   * Constructor.
   * @param checkFactory Factory.
   * @param fileLinesContextFactory Factory.
   */
  public MagikSquidSensor(CheckFactory checkFactory,
                          FileLinesContextFactory fileLinesContextFactory,
                          NoSonarFilter noSonarFilter) {
    this.checks = checkFactory
        .<MagikCheck>create(CheckList.REPOSITORY_KEY)
        .addAnnotatedChecks((Iterable) CheckList.getChecks());
    this.fileLinesContextFactory = fileLinesContextFactory;
    this.noSonarFilter = noSonarFilter;
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor
        .onlyOnLanguage(Magik.KEY)
        .name("Magik Squid Sensor")
        .onlyOnFileType(Type.MAIN);
  }

  @Override
  public void execute(SensorContext context) {
    FileSystem fileSystem = context.fileSystem();
    FilePredicates predicates = fileSystem.predicates();

    FilePredicate filePredicate = predicates.and(
        predicates.hasType(InputFile.Type.MAIN),
        predicates.hasLanguage(Magik.KEY));

    List<InputFile> inputFiles = new ArrayList<>();
    fileSystem.inputFiles(filePredicate).forEach(inputFiles::add);

    long period = 100;
    ProgressReport progressReport = new ProgressReport(
        "Report about progress of Sonar Magik analyzer", period);
    List<String> filenames = inputFiles.stream()
        .map(InputFile::toString)
        .collect(Collectors.toList());
    progressReport.start(filenames);

    Charset fsCharset = fileSystem.encoding();
    for (InputFile inputFile : inputFiles) {
      Path path = Paths.get(inputFile.toString());
      Charset charset = FileCharsetDeterminer.determineCharset(path, fsCharset);
      scanMagikFile(context, charset, inputFile);

      progressReport.nextFile();
    }

    progressReport.stop();
  }

  private void scanMagikFile(SensorContext context, Charset charset, InputFile inputFile) {
    LOGGER.debug("Scanning magik file: {}", inputFile);

    // read contents
    String fileContent;
    try {
      fileContent = inputFile.contents();
    } catch (IOException ex) {
      throw new IllegalStateException("Cannot read " + inputFile, ex);
    }

    // parse and save metrics
    LOGGER.debug("Parse and save measures");
    MagikParser parser = new MagikParser(charset);
    MagikVisitorContext visitorContext;
    try {
      AstNode tree = parser.parse(fileContent);
      visitorContext = new MagikVisitorContext(fileContent, tree);
      saveMetrics(context, inputFile, visitorContext);
    } catch (RecognitionException ex) {
      visitorContext = new MagikVisitorContext(fileContent, ex);
      LOGGER.error("Unable to parse file: {}", inputFile);
      LOGGER.error("Exception", ex);
    }

    // save issues
    LOGGER.debug("Running checks");
    for (MagikCheck check : checks.all()) {
      LOGGER.debug("Running check: {}", check);
      List<MagikIssue> issues = check.scanFileForIssues(visitorContext);
      saveIssues(context, check, issues, inputFile);
    }

    // save highlighted tokens
    LOGGER.debug("Saving highlighted tokens");
    MagikVisitor tokensVisitor = new MagikHighlighterVisitor(context, inputFile);
    tokensVisitor.scanFile(visitorContext);

    // save CPD tokens
    LOGGER.debug("Saving CPD tokens");
    saveCpdTokens(context, visitorContext, inputFile);
  }

  private void saveMetrics(SensorContext context,
                           InputFile inputFile,
                           MagikVisitorContext visitorContext) {
    FileMetrics metrics = new FileMetrics(visitorContext, true);

    // metrics on file
    saveMetric(context, inputFile, CoreMetrics.NCLOC, metrics.linesOfCode().size());
    saveMetric(context, inputFile, CoreMetrics.COMMENT_LINES, metrics.commentLines().size());
    saveMetric(context, inputFile, CoreMetrics.CLASSES, metrics.numberOfExemplars());
    saveMetric(context, inputFile, CoreMetrics.FUNCTIONS, metrics.numberOfMethods()
        + metrics.numberOfProcedures());
    saveMetric(context, inputFile, CoreMetrics.STATEMENTS, metrics.numberOfStatements());
    saveMetric(context, inputFile, CoreMetrics.COMPLEXITY, metrics.fileComplexity());

    // metrics on lines
    FileLinesContext fileLinesContext = fileLinesContextFactory.createFor(inputFile);
    metrics.linesOfCode().forEach(
        line -> fileLinesContext.setIntValue(CoreMetrics.NCLOC_DATA_KEY, line, 1));
    metrics.commentLines().forEach(
        line -> fileLinesContext.setIntValue(CoreMetrics.COMMENT_LINES_DATA_KEY, line, 1));
    metrics.executableLines().forEach(
        line -> fileLinesContext.setIntValue(CoreMetrics.EXECUTABLE_LINES_DATA_KEY, line, 1));
    fileLinesContext.save();

    // no sonar filter
    noSonarFilter.noSonarInFile(inputFile, metrics.nosonarLines());
  }

  private <T extends Serializable> void saveMetric(SensorContext context,
                                                   InputFile inputFile,
                                                   Metric metric,
                                                   T value) {
    LOGGER.debug("Saving metric, file: {}, metric: {} value: {}", inputFile, metric, value);

    context.<T>newMeasure()
        .withValue(value)
        .forMetric(metric)
        .on(inputFile)
        .save();
  }

  private void saveIssues(SensorContext context,
                          MagikCheck check,
                          List<MagikIssue> issues,
                          InputFile inputFile) {
    for (MagikIssue magikIssue : issues) {
      LOGGER.debug("Saving issue, file: {}, issue: {}", inputFile, magikIssue);

      RuleKey ruleKey = checks.ruleKey(check);
      NewIssue issue = context.newIssue();
      NewIssueLocation location = issue.newLocation()
          .on(inputFile)
          .message(magikIssue.message());
      Integer line = magikIssue.line();
      if (line != null) {
        location.at(inputFile.selectLine(line));
      }
      Double cost = magikIssue.cost();
      if (cost != null) {
        issue.gap(cost);
      }
      issue.at(location).forRule(ruleKey).save();
    }
  }

  private void saveCpdTokens(SensorContext context,
                             MagikVisitorContext visitorContext,
                             InputFile inputFile) {
    LOGGER.debug("Saving CPD tokens, file: {}", inputFile);

    NewCpdTokens newCpdTokens = context.newCpdTokens().onFile(inputFile);

    List<Token> tokens = visitorContext.tokens();
    for (Token token : tokens) {
      TokenLocation location = new TokenLocation(token);
      newCpdTokens.addToken(
          location.line(), location.column(),
          location.endLine(), location.endColumn(),
          token.getValue());
    }
    newCpdTokens.save();
  }
}
