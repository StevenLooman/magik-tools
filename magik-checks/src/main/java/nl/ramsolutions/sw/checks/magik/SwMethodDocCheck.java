package nl.ramsolutions.sw.checks.magik;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.checks.DisabledByDefault;
import nl.ramsolutions.sw.checks.MagikCheck;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.parser.MagikCommentExtractor;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;

/** Check if method docs are valid, according to SW style. */
@DisabledByDefault
@Rule(key = SwMethodDocCheck.CHECK_KEY)
public class SwMethodDocCheck extends MagikCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "SwMethodDoc";

  private static final String MESSAGE = "No or invalid method doc: %s.";

  private static final boolean DEFAULT_ALLOW_BLANK_METHOD_DOC = false;
  private static final String PARAMETER_REGEXP = "[ \t]?([\\p{Lu}\\d_?]+)[^\\p{Lu}\\d_?]?";
  private static final Pattern PARAMETER_PATTERN = Pattern.compile(PARAMETER_REGEXP);

  /** Allow blank method doc. */
  @RuleProperty(
      key = "allow blank method doc",
      defaultValue = "" + DEFAULT_ALLOW_BLANK_METHOD_DOC,
      description = "Allow blank method doc",
      type = "BOOLEAN")
  @SuppressWarnings("checkstyle:VisibilityModifier")
  public boolean allowBlankMethodDoc = DEFAULT_ALLOW_BLANK_METHOD_DOC;

  @Override
  protected void walkPreMethodDefinition(final AstNode node) {
    final MethodDefinitionNodeHelper helper = new MethodDefinitionNodeHelper(node);
    final AstNode methodNameNode = helper.getMethodNameNode();
    final String methodDoc = this.extractDoc(node);
    if (methodDoc.isBlank() && !this.allowBlankMethodDoc) {
      final String message = MESSAGE.formatted("all");
      this.addIssue(methodNameNode, message);
      return;
    }

    final Set<String> methodParameters = this.getMethodParameters(node);
    final Set<String> docParameters = this.getDocParameters(node);
    methodParameters.removeAll(docParameters);
    for (final String missing : methodParameters) {
      final String message = MESSAGE.formatted(missing);
      this.addIssue(methodNameNode, message);
    }
  }

  private Set<String> getMethodParameters(final AstNode node) {
    final MethodDefinitionNodeHelper helper = new MethodDefinitionNodeHelper(node);
    final Map<String, AstNode> parameterNodes = helper.getParameterNodes();
    final Set<String> parameterNames = parameterNodes.keySet();
    return parameterNames.stream()
        .map(String::toUpperCase)
        .collect(Collectors.toCollection(HashSet::new));
  }

  private String extractDoc(final AstNode node) {
    return MagikCommentExtractor.extractDocCommentTokens(node)
        .map(Token::getValue)
        .map(comment -> comment.substring("##".length()))
        .collect(Collectors.joining("\n"));
  }

  private Set<String> getDocParameters(final AstNode node) {
    final String methodDoc = this.extractDoc(node);
    final Set<String> uppercased = new HashSet<>();

    final Matcher matcher = PARAMETER_PATTERN.matcher(methodDoc);
    while (matcher.find()) {
      final String name = matcher.group(1);
      uppercased.add(name);
    }

    return uppercased;
  }
}
