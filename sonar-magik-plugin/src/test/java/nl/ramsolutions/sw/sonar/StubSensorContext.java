package nl.ramsolutions.sw.sonar;

import java.io.InputStream;
import java.io.Serializable;
import org.sonar.api.SonarRuntime;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputModule;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.cache.ReadCache;
import org.sonar.api.batch.sensor.cache.WriteCache;
import org.sonar.api.batch.sensor.code.NewSignificantCode;
import org.sonar.api.batch.sensor.coverage.NewCoverage;
import org.sonar.api.batch.sensor.cpd.NewCpdTokens;
import org.sonar.api.batch.sensor.error.NewAnalysisError;
import org.sonar.api.batch.sensor.highlighting.NewHighlighting;
import org.sonar.api.batch.sensor.issue.NewExternalIssue;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueResolution;
import org.sonar.api.batch.sensor.measure.NewMeasure;
import org.sonar.api.batch.sensor.rule.NewAdHocRule;
import org.sonar.api.batch.sensor.symbol.NewSymbolTable;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.Settings;
import org.sonar.api.scanner.fs.InputProject;
import org.sonar.api.utils.Version;

/**
 * {@link SensorContext} handing out a {@link RecordingCpdTokens}. Methods other than {@link
 * #newCpdTokens()} throw {@link UnsupportedOperationException}.
 */
public class StubSensorContext implements SensorContext {

  private final RecordingCpdTokens cpdTokens = new RecordingCpdTokens();

  /**
   * Get the {@link RecordingCpdTokens} handed out by this context.
   *
   * @return Recording CPD tokens.
   */
  public RecordingCpdTokens getCpdTokens() {
    return this.cpdTokens;
  }

  @Override
  public NewCpdTokens newCpdTokens() {
    return this.cpdTokens;
  }

  @Override
  @Deprecated
  public Settings settings() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Configuration config() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean canSkipUnchangedFiles() {
    throw new UnsupportedOperationException();
  }

  @Override
  public FileSystem fileSystem() {
    throw new UnsupportedOperationException();
  }

  @Override
  public ActiveRules activeRules() {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public InputModule module() {
    throw new UnsupportedOperationException();
  }

  @Override
  public InputProject project() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Version getSonarQubeVersion() {
    throw new UnsupportedOperationException();
  }

  @Override
  public SonarRuntime runtime() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isCancelled() {
    throw new UnsupportedOperationException();
  }

  @Override
  public <G extends Serializable> NewMeasure<G> newMeasure() {
    throw new UnsupportedOperationException();
  }

  @Override
  public NewIssue newIssue() {
    throw new UnsupportedOperationException();
  }

  @Override
  public NewExternalIssue newExternalIssue() {
    throw new UnsupportedOperationException();
  }

  @Override
  public NewIssueResolution newIssueResolution() {
    throw new UnsupportedOperationException();
  }

  @Override
  public NewAdHocRule newAdHocRule() {
    throw new UnsupportedOperationException();
  }

  @Override
  public NewHighlighting newHighlighting() {
    throw new UnsupportedOperationException();
  }

  @Override
  public NewSymbolTable newSymbolTable() {
    throw new UnsupportedOperationException();
  }

  @Override
  public NewCoverage newCoverage() {
    throw new UnsupportedOperationException();
  }

  @Override
  public NewAnalysisError newAnalysisError() {
    throw new UnsupportedOperationException();
  }

  @Override
  public NewSignificantCode newSignificantCode() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void addContextProperty(final String key, final String value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void markForPublishing(final InputFile inputFile) {
    throw new UnsupportedOperationException();
  }

  @Override
  public WriteCache nextCache() {
    throw new UnsupportedOperationException();
  }

  @Override
  public ReadCache previousCache() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isCacheEnabled() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void markAsUnchanged(final InputFile inputFile) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isFeatureAvailable(final String feature) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void addTelemetryProperty(final String key, final String value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void addAnalysisData(final String key, final String contentType, final InputStream data) {
    throw new UnsupportedOperationException();
  }
}
