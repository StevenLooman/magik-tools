package org.stevenlooman.sw.magik;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;
import org.sonar.check.Rule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class MagikCheck extends MagikVisitor {
  private List<MagikIssue> issues = new ArrayList<>();

  /**
   * Is this a templated check.
   */
  public abstract boolean isTemplatedCheck();

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

  /**
   * Add a new issue.
   * @param message Message of issue.
   * @param line Line of issue.
   * @param column Column of issue.
   */
  public void addIssue(String message, int line, int column) {
    issues.add(MagikIssue.lineColumnIssue(line, column, message, this));
  }

  /**
   * Add a new issue.
   * @param message Message of issue.
   * @param node AstNode of issue.
   */
  public void addIssue(String message, AstNode node) {
    Token token = node.getToken();
    addIssue(message, token);
  }

  /**
   * Add a new issue.
   * @param message Message of issue.
   * @param token Token of issue.
   */
  public void addIssue(String message, Token token) {
    addIssue(message, token.getLine(), token.getColumn());
  }

  /**
   * Add a new issue at the file.
   * @param message Message of the issue.
   */
  public void addFileIssue(String message) {
    issues.add(MagikIssue.fileIssue(message, this));
  }

  /**
   * Get the check key.
   * @return The check key.
   */
  public String getCheckKey() {
    Rule annotation = getClass().getAnnotation(Rule.class);
    return annotation.key();
  }

  /**
   * Get the check key, kebab-cased.
   * @return The check key, kebab-cased.
   */
  public String getCheckKeyKebabCase() {
    Pattern pattern = Pattern.compile("(?=[A-Z][a-z])");
    String checkKey = getCheckKey();
    Matcher matcher = pattern.matcher(checkKey);
    String checkKeyKebab = matcher.replaceAll("-").toLowerCase();
    if (checkKeyKebab.startsWith("-")) {
      checkKeyKebab = checkKeyKebab.substring(1);
    }
    return checkKeyKebab;
  }

}
