package nl.ramsolutions.sw.checks.loadlist;

import com.sonar.sslr.api.AstNode;
import nl.ramsolutions.sw.checks.LoadListCheck;
import nl.ramsolutions.sw.loadlist.api.LoadListGrammar;
import org.sonar.check.Rule;

/** Check that a load_list.txt/patch_list.txt file has no syntax errors. */
@Rule(key = LoadListSyntaxErrorCheck.CHECK_KEY)
public class LoadListSyntaxErrorCheck extends LoadListCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "LoadListSyntaxError";

  private static final String MESSAGE = "Invalid syntax.";

  @Override
  protected void scanFile() {
    final AstNode topNode = this.getLoadListFile().getTopNode();
    topNode
        .getDescendants(LoadListGrammar.SYNTAX_ERROR)
        .forEach(node -> this.addIssue(node, MESSAGE));
  }
}
