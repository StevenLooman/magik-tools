package nl.ramsolutions.sw.magik.checks.checks;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import nl.ramsolutions.sw.magik.checks.MagikIssue;

/** Base class for MagikCheck tests. */
class MagikCheckTestBase {

  protected static final URI DEFAULT_URI = URI.create("memory://source.magik");
  ;

  /**
   * VSCode runs from module directory, mvn runs from project directory.
   *
   * @return Proper {@link Path} to file.
   */
  protected Path getPath(final Path relativePath) {
    final Path path = Path.of(".").toAbsolutePath().getParent();
    if (path.endsWith("magik-checks")) {
      return Path.of("..").resolve(relativePath);
    }
    return Path.of(".").resolve(relativePath);
  }

  /**
   * Read file contents.
   *
   * @param path Path to file.
   * @param charset Charset to use.
   * @return File contents.
   * @throws IOException -
   */
  protected static String readFileContents(final Path path, final Charset charset)
      throws IOException {
    final File file = path.toFile();
    final FileInputStream fis = new FileInputStream(file);
    final byte[] data = new byte[(int) file.length()];
    fis.read(data);
    fis.close();

    return new String(data, charset);
  }

  /**
   * Run check on code.
   *
   * @param code Code.
   * @param check Check to run.
   * @return List with issues.
   * @throws IllegalArgumentException -
   */
  protected List<MagikIssue> runCheck(final String code, final MagikCheck check)
      throws IllegalArgumentException {
    final MagikFile magikFile = new MagikFile(MagikCheckTestBase.DEFAULT_URI, code);
    final List<MagikIssue> issues = check.scanFileForIssues(magikFile);
    return issues;
  }

  /**
   * Run check on path.
   *
   * @param path Path to source file.
   * @param check Check to run.
   * @return List with issues.
   * @throws IllegalArgumentException -
   * @throws IOException -
   */
  protected List<MagikIssue> runCheck(final Path path, final MagikCheck check)
      throws IllegalArgumentException, IOException {
    final Path fixedPath = this.getPath(path);
    final URI uri = fixedPath.toUri();
    final String code = Files.readString(fixedPath);
    final MagikFile magikFile = new MagikFile(uri, code);
    final List<MagikIssue> issues = check.scanFileForIssues(magikFile);
    return issues;
  }
}
