package org.stevenlooman.sw.magik.checks;

import com.sonar.sslr.api.AstNode;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.stevenlooman.sw.magik.MagikCheck;
import org.stevenlooman.sw.magik.MagikVisitorContext;
import org.stevenlooman.sw.magik.analysis.scope.GlobalScope;
import org.stevenlooman.sw.magik.analysis.scope.Scope;
import org.stevenlooman.sw.magik.analysis.scope.ScopeEntry;

import java.util.Arrays;
import java.util.List;

@Rule(key = VariableNamingCheck.CHECK_KEY)
public class VariableNamingCheck extends MagikCheck {

  private static final String MESSAGE = "Give the variable \"%s\" a proper descriptive name.";
  public static final String CHECK_KEY = "VariableNaming";
  private static final int MIN_LENGTH = 3;

  public static final String DEFAULT_WHITELIST =
      "x,y,z";

  @RuleProperty(
      key = "whitelist",
      description = "Whitelist (comma separated) of variable names to allow/ignore.")
  public String whitelist = DEFAULT_WHITELIST;

  private String stripPrefix(String identifier) {
    String lowered = identifier.toLowerCase();
    if (lowered.startsWith("p_")
        || lowered.startsWith("l_")
        || lowered.startsWith("i_")
        || lowered.startsWith("c_")) {
      return identifier.substring(2);
    }
    return identifier;
  }

  private List<String> whitelist() {
    return Arrays.asList(whitelist.split(","));
  }

  @Override
  protected void walkPostMagik(AstNode node) {
    MagikVisitorContext context = getContext();
    GlobalScope globalScope = context.getGlobalScope();
    for (Scope scope : globalScope.getSelfAndDescendantScopes()) {
      for (ScopeEntry scopeEntry : scope.getScopeEntries()) {
        if (scopeEntry.getType() == ScopeEntry.Type.LOCAL
            || scopeEntry.getType() == ScopeEntry.Type.DEFINITION
            || scopeEntry.getType() == ScopeEntry.Type.PARAMETER) {
          String identifier = scopeEntry.getIdentifier();

          if (!isValidName(identifier)) {
            String message = String.format(MESSAGE, identifier);
            AstNode identifierNode = scopeEntry.getNode();
            addIssue(message, identifierNode);
          }
        }
      }
    }
  }

  private boolean isValidName(String identifier) {
    String strippedIdentifier = stripPrefix(identifier);
    List<String> whitelist = whitelist();
    return whitelist.contains(strippedIdentifier)
           || strippedIdentifier.length() >= MIN_LENGTH;
  }

}
