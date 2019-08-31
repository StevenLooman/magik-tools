package org.stevenlooman.sw.magik.checks;

import com.sonar.sslr.api.AstNode;
import org.stevenlooman.sw.magik.MagikCheck;
import org.stevenlooman.sw.magik.MagikIssue;
import org.stevenlooman.sw.magik.MagikVisitorContext;
import org.stevenlooman.sw.magik.parser.MagikParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class MagikCheckTestBase {

  /**
   * VSCode runs from module directory, mvn runs from project directory.
   *
   * @return Proper {{Path}} to file.
   */
  protected Path getPath(Path relativePath) {
    Path path = Paths.get(".").toAbsolutePath().getParent();
    if (path.endsWith("magik-checks")) {
      return Paths.get("..").resolve(relativePath);
    }
    return Paths.get(".").resolve(relativePath);
  }

  protected static String readFileContents(Path path, Charset charset) throws IOException {
    File file = path.toFile();
    FileInputStream fis = new FileInputStream(file);
    byte[] data = new byte[(int) file.length()];
    fis.read(data);
    fis.close();

    return new String(data, charset);
  }

  protected static MagikVisitorContext createContext(String code)
      throws IllegalArgumentException {
    MagikParser parser = new MagikParser(Charset.forName("UTF-8"));
    AstNode root = parser.parseSafe(code);
    return new MagikVisitorContext(code, root);
  }

  protected static MagikVisitorContext createFileContext(Path path)
      throws IllegalArgumentException, IOException {
    Charset charset = Charset.forName("UTF-8");
    MagikParser parser = new MagikParser(charset);
    AstNode root = parser.parseSafe(path);
    String fileContent = readFileContents(path, charset);
    return new MagikVisitorContext(path, fileContent, root);
  }

  protected List<MagikIssue> runCheck(String code, MagikCheck check)
      throws IllegalArgumentException {
    MagikVisitorContext context = createContext(code);
    List<MagikIssue> issues = check.scanFileForIssues(context);
    return issues;
  }

  protected List<MagikIssue> runCheck(Path path, MagikCheck check)
      throws IllegalArgumentException, IOException {
    Path fixedPath = getPath(path);
    MagikVisitorContext context = createFileContext(fixedPath);
    List<MagikIssue> issues = check.scanFileForIssues(context);
    return issues;
  }
}
