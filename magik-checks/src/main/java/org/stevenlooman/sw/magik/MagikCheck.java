package org.stevenlooman.sw.magik;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class MagikCheck extends MagikVisitor {
  private List<MagikIssue> issues = new ArrayList<>();

  public final static String CHECK_KEY = "";

  /**
   * Scan the file from the context for issues.
   * @param context Context to use.
   * @return List issues.
   */
  public List<MagikIssue> scanFileForIssues(MagikVisitorContext context) {
    issues = new ArrayList<>();
    scanFile(context);
    return Collections.unmodifiableList(issues);
  }

  public void addIssue(String message, int line, int column) {
    issues.add(MagikIssue.lineColumnIssue(line, column, message, this));
  }

  public void addIssue(String message, AstNode node) {
    Token token = node.getToken();
    addIssue(message, token.getLine(), token.getColumn());
  }

  public void addIssue(String message, Token token) {
    addIssue(message, token.getLine(), token.getColumn());
  }

  public void addFileIssue(String message) {
    issues.add(MagikIssue.fileIssue(message, this));
  }

}
