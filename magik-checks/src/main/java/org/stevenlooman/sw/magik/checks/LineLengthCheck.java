package org.stevenlooman.sw.magik.checks;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.stevenlooman.sw.magik.MagikCheck;
import org.stevenlooman.sw.magik.MagikVisitorContext;

import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;

@Rule(key = LineLengthCheck.CHECK_KEY)
public class LineLengthCheck extends MagikCheck {
  public static final String CHECK_KEY = "LineLength";
  private static final int DEFAULT_MAX_LINE_LENGTH = 120;
  private static final String MESSAGE = "Line is too long";

  @RuleProperty(
      key = "line length",
      defaultValue = "" + DEFAULT_MAX_LINE_LENGTH,
      description = "Maximum number of characters on a single line")
  public int maxLineLength = DEFAULT_MAX_LINE_LENGTH;

  @Override
  public List<AstNodeType> subscribedTo() {
    return Collections.emptyList();
  }

  @Override
  public void visitFile(@Nullable AstNode node) {
    MagikVisitorContext context = getContext();
    String fileContents = context.fileContent();

    int lineNo = 0;
    for (String line: fileContents.split("\n")) {
      line = line.trim();
      line = line.replaceAll("\t", "        ");  // tab width of 8
      ++lineNo;
      if (line.length() > maxLineLength + 1) {
        addIssue(MESSAGE, lineNo, maxLineLength);
      }
    }
  }
}
