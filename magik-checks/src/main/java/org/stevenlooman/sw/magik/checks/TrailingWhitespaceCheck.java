package org.stevenlooman.sw.magik.checks;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;
import org.sonar.check.Rule;
import org.stevenlooman.sw.magik.MagikCheck;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

@Rule(key = TrailingWhitespaceCheck.CHECK_KEY)
public class TrailingWhitespaceCheck extends MagikCheck {

  private static final String MESSAGE = "Remove the trailing whitespace at line \"%s\".";
  public static final String CHECK_KEY = "TrailingWhitespace";

  @Override
  public List<AstNodeType> subscribedTo() {
    return Collections.emptyList();
  }

  public void visitFile(@Nullable AstNode node) {
    String contents = getContext().fileContent();
    String[] lines = contents.split("\n");
    for (int lineNo = 0; lineNo < lines.length; ++lineNo) {
      String line = lines[lineNo];

      // strip \r, if any
      if (line.endsWith("\r")) {
        line = line.substring(0, line.length() - 1);
      }

      if (line.endsWith(" ")
          || line.endsWith("\t")) {
        String message = String.format(MESSAGE, lineNo);
        addIssue(message, lineNo, line.length());
      }
    }
  }

}
