package org.stevenlooman.sw.magik.checks;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.stevenlooman.sw.magik.MagikCheck;
import org.stevenlooman.sw.magik.api.MagikGrammar;

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

  @Override
  public List<AstNodeType> subscribedTo() {
    return Arrays.asList(
        MagikGrammar.VARIABLE_DECLARATION,
        MagikGrammar.MULTI_VARIABLE_DECLARATION,
        MagikGrammar.PARAMETER
    );
  }

  @Override
  public void visitNode(AstNode node) {
    List<AstNode> identifierNodes;
    if (node.getType() == MagikGrammar.MULTI_VARIABLE_DECLARATION) {
      identifierNodes = node
          .getFirstChild(MagikGrammar.IDENTIFIERS_WITH_GATHER)
          .getChildren(MagikGrammar.IDENTIFIER);
    } else {
      identifierNodes = node.getChildren(MagikGrammar.IDENTIFIER);
    }

    for (AstNode identifierNode : identifierNodes) {
      String identifier = identifierNode.getTokenValue();
      String strippedIdentifier = stripPrefix(identifier);

      List<String> whitelist = whitelist();
      if (whitelist.contains(strippedIdentifier)) {
        continue;
      }

      if (strippedIdentifier.length() >= MIN_LENGTH) {
        continue;
      }

      String message = String.format(MESSAGE, identifier);
      addIssue(message, identifierNode);
    }
  }

  private String stripPrefix(String identifier) {
    String lowered = identifier.toLowerCase();
    if (lowered.startsWith("p_")
        || lowered.startsWith("l_")
        || lowered.startsWith("i_")) {
      return identifier.substring(2);
    }
    return identifier;
  }

  private List<String> whitelist() {
    return Arrays.asList(whitelist.split(","));
  }

}
