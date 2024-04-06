package nl.ramsolutions.sw.magik.typedchecks.checks;

import com.sonar.sslr.api.AstNode;
import java.util.Map;
import java.util.Objects;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeStringResolver;
import nl.ramsolutions.sw.magik.analysis.typing.reasoner.LocalTypeReasonerState;
import nl.ramsolutions.sw.magik.api.TypeDocGrammar;
import nl.ramsolutions.sw.magik.parser.TypeDocParser;
import nl.ramsolutions.sw.magik.typedchecks.MagikTypedCheck;
import nl.ramsolutions.sw.magik.utils.StreamUtils;
import org.sonar.check.Rule;

/** Check to check if the @return types from method doc matches the reasoned return types. */
@Rule(key = MethodReturnTypesMatchDocTypedCheck.CHECK_KEY)
public class MethodReturnTypesMatchDocTypedCheck extends MagikTypedCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "MethodReturnTypesMatchDoc";

  private static final String MESSAGE =
      "@return type(s) (%s) do not match method return type(s) (%s).";

  @Override
  protected void walkPostMethodDefinition(final AstNode node) {
    final MethodDefinitionNodeHelper helper = new MethodDefinitionNodeHelper(node);
    if (helper.isAbstractMethod()) {
      return;
    }

    final ExpressionResultString methodResultString = this.extractReasonedResult(node);
    final Map<AstNode, TypeString> typeDocNodes = this.extractMethodDocResult(node);
    final TypeStringResolver resolver = this.getTypeStringResolver();
    StreamUtils.zip(methodResultString.stream(), typeDocNodes.entrySet().stream())
        .forEach(
            entry -> {
              if (entry.getKey() == null || entry.getValue() == null) {
                // Only bother with type-doc returns.
                return;
              }

              final TypeString methodReturnTypeString = entry.getKey();
              final ExemplarDefinition methodExemplarDef =
                  resolver.getExemplarDefinition(methodReturnTypeString);

              final Map.Entry<AstNode, TypeString> typeDocEntry = entry.getValue();
              final TypeString docReturnTypeString = typeDocEntry.getValue();
              final ExemplarDefinition docExemplarDef =
                  resolver.getExemplarDefinition(docReturnTypeString);

              if (Objects.equals(methodExemplarDef, docExemplarDef)) {
                return;
              }

              final String message =
                  String.format(
                      MESSAGE,
                      docReturnTypeString.getFullString(),
                      methodReturnTypeString.getFullString());
              final AstNode returnTypeNode = typeDocEntry.getKey();
              final AstNode typeValueNode = returnTypeNode.getFirstChild(TypeDocGrammar.TYPE_VALUE);
              this.addIssue(typeValueNode, message);
            });
  }

  private ExpressionResultString extractReasonedResult(final AstNode node) {
    final LocalTypeReasonerState reasonerState = this.getTypeReasonerState();
    return reasonerState.getNodeType(node);
  }

  private Map<AstNode, TypeString> extractMethodDocResult(final AstNode node) {
    final TypeDocParser docParser = new TypeDocParser(node);
    return docParser.getReturnTypeNodes();
  }
}
