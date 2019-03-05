package org.stevenlooman.sw.magik.checks;

import com.google.common.base.Charsets;
import com.sonar.sslr.api.AstNode;
import org.stevenlooman.sw.magik.MagikVisitorContext;
import org.stevenlooman.sw.magik.MagikIssue;
import org.stevenlooman.sw.magik.MagikCheck;
import org.stevenlooman.sw.magik.parser.MagikParser;

import java.util.List;

@SuppressWarnings("squid:S2187")
public class MagikCheckTest {
  protected static MagikVisitorContext createContext(String code) throws IllegalArgumentException {
    MagikParser parser = new MagikParser(Charsets.UTF_8);
    AstNode root = parser.parse(code);
    if (root.getChildren().isEmpty()) {
      throw new IllegalArgumentException("Unable to parse code");
    }
    return new MagikVisitorContext(code, root);
  }

  protected List<MagikIssue> runCheck(String code, MagikCheck check) {
    MagikVisitorContext context = createContext(code);
    List<MagikIssue> issues = check.scanFileForIssues(context);
    return issues;
  }
}
