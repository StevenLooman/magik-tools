package nl.ramsolutions.sw.sonar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.sensor.cpd.NewCpdTokens;

/** {@link NewCpdTokens} recording the tokens it is given. */
public class RecordingCpdTokens implements NewCpdTokens {

  /** Token as recorded by {@link RecordingCpdTokens}. */
  public record CpdToken(
      int startLine, int startLineOffset, int endLine, int endLineOffset, String value) {}

  private final List<CpdToken> tokens = new ArrayList<>();
  private InputFile inputFile;
  private boolean saved;

  /**
   * Get the file the tokens were recorded for.
   *
   * @return Input file, or null if {@link #onFile(InputFile)} was never called.
   */
  public InputFile getInputFile() {
    return this.inputFile;
  }

  /**
   * Get the recorded tokens, in the order they were added.
   *
   * @return Recorded tokens.
   */
  public List<CpdToken> getTokens() {
    return Collections.unmodifiableList(this.tokens);
  }

  /**
   * Test whether {@link #save()} was called.
   *
   * @return True if saved, false otherwise.
   */
  public boolean isSaved() {
    return this.saved;
  }

  @Override
  public NewCpdTokens onFile(final InputFile inputFile) {
    this.inputFile = inputFile;
    return this;
  }

  @Override
  public NewCpdTokens addToken(final TextRange range, final String value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public NewCpdTokens addToken(
      final int startLine,
      final int startLineOffset,
      final int endLine,
      final int endLineOffset,
      final String value) {
    this.tokens.add(new CpdToken(startLine, startLineOffset, endLine, endLineOffset, value));
    return this;
  }

  @Override
  public void save() {
    this.saved = true;
  }
}
