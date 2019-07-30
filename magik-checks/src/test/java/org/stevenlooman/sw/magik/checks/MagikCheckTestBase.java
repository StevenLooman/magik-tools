package org.stevenlooman.sw.magik.checks;

import com.sonar.sslr.api.AstNode;
import org.stevenlooman.sw.magik.MagikCheck;
import org.stevenlooman.sw.magik.MagikIssue;
import org.stevenlooman.sw.magik.MagikVisitorContext;
import org.stevenlooman.sw.magik.parser.MagikParser;

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

  protected static MagikVisitorContext createContext(String code)
      throws IllegalArgumentException {
    MagikParser parser = new MagikParser(Charset.forName("UTF-8"));
    AstNode root = parser.parse(code);
    return new MagikVisitorContext(code, root);
  }

  protected static MagikVisitorContext createFileContext(Path path)
      throws IllegalArgumentException {
    MagikParser parser = new MagikParser(Charset.forName("UTF-8"));
    AstNode root = parser.parse(path);
    if (root.getChildren().isEmpty()) {
      throw new IllegalArgumentException("Unable to parse code");
    }
    return new MagikVisitorContext(path, root);
  }

  protected List<MagikIssue> runCheck(String code, MagikCheck check)
      throws IllegalArgumentException {
    MagikVisitorContext context = createContext(code);
    List<MagikIssue> issues = check.scanFileForIssues(context);
    return issues;
  }

  protected List<MagikIssue> runCheck(Path path, MagikCheck check)
      throws IllegalArgumentException {
    Path fixedPath = getPath(path);
    MagikVisitorContext context = createFileContext(fixedPath);
    List<MagikIssue> issues = check.scanFileForIssues(context);
    return issues;
  }
}
