package nl.ramsolutions.sw.sonar;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Path;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextPointer;
import org.sonar.api.batch.fs.TextRange;

/**
 * {@link InputFile} backed by a path and its contents. Methods which are not backed by the given
 * state throw {@link UnsupportedOperationException}.
 */
public class StubInputFile implements InputFile {

  private final String key;
  private final Path path;
  private final Charset charset;
  private final String contents;

  /**
   * Constructor.
   *
   * @param key Key of the file.
   * @param path Path to the file.
   * @param charset Charset of the file.
   * @param contents Contents of the file.
   */
  public StubInputFile(
      final String key, final Path path, final Charset charset, final String contents) {
    this.key = key;
    this.path = path;
    this.charset = charset;
    this.contents = contents;
  }

  @Override
  public String key() {
    return this.key;
  }

  @Override
  public boolean isFile() {
    return true;
  }

  @Override
  @Deprecated
  public Path path() {
    return this.path;
  }

  @Override
  public URI uri() {
    return this.path.toUri();
  }

  @Override
  public String filename() {
    return this.path.getFileName().toString();
  }

  @Override
  public Charset charset() {
    return this.charset;
  }

  @Override
  public String contents() {
    return this.contents;
  }

  @Override
  public String toString() {
    return this.key;
  }

  @Override
  @Deprecated
  public String relativePath() {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public String absolutePath() {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public File file() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String language() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Type type() {
    throw new UnsupportedOperationException();
  }

  @Override
  public InputStream inputStream() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Status status() {
    throw new UnsupportedOperationException();
  }

  @Override
  public int lines() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isEmpty() {
    throw new UnsupportedOperationException();
  }

  @Override
  public TextPointer newPointer(final int line, final int lineOffset) {
    throw new UnsupportedOperationException();
  }

  @Override
  public TextRange newRange(final TextPointer start, final TextPointer end) {
    throw new UnsupportedOperationException();
  }

  @Override
  public TextRange newRange(
      final int startLine, final int startLineOffset, final int endLine, final int endLineOffset) {
    throw new UnsupportedOperationException();
  }

  @Override
  public TextRange selectLine(final int line) {
    throw new UnsupportedOperationException();
  }
}
