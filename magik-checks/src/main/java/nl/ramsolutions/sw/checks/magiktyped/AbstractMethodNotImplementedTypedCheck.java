package nl.ramsolutions.sw.checks.magiktyped;

import com.sonar.sslr.api.AstNode;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import nl.ramsolutions.sw.checks.MagikTypedCheck;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.helpers.ArgumentsNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeStringResolver;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import org.sonar.check.Rule;

/** Check if exemplars/mixins implement all inherited abstract methods. */
@Rule(key = AbstractMethodNotImplementedTypedCheck.CHECK_KEY)
public class AbstractMethodNotImplementedTypedCheck extends MagikTypedCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "AbstractMethodNotImplemented";

  private static final String MESSAGE = "Abstract method %s.%s is not implemented by %s.";

  @Override
  protected void walkPostMagik(final AstNode node) {
    final TypeStringResolver resolver = this.getTypeStringResolver();
    this.getMagikFile().getMagikDefinitions().stream()
        .filter(ExemplarDefinition.class::isInstance)
        .map(ExemplarDefinition.class::cast)
        .forEach(exemplarDefinition -> this.checkExemplarDefinition(resolver, exemplarDefinition));
  }

  private void checkExemplarDefinition(
      final TypeStringResolver resolver, final ExemplarDefinition exemplarDefinition) {
    final TypeString typeStr = exemplarDefinition.getTypeString();

    // Collect all abstract methods inherited from ancestors, keyed by name so the same method
    // reachable via multiple ancestors is only reported once.
    final Map<String, MethodDefinition> abstractMethods = new HashMap<>();
    for (final TypeString ancestorTypeStr : resolver.getAllAncestors(typeStr)) {
      for (final MethodDefinition ancestorMethod :
          resolver.getRespondingMethodDefinitions(ancestorTypeStr)) {
        if (ancestorMethod.getModifiers().contains(MethodDefinition.Modifier.ABSTRACT)) {
          abstractMethods.putIfAbsent(ancestorMethod.getMethodName(), ancestorMethod);
        }
      }
    }

    for (final MethodDefinition abstractMethod : abstractMethods.values()) {
      final String methodName = abstractMethod.getMethodName();

      // Check if the exemplar has a concrete implementation.
      final Collection<MethodDefinition> implementations =
          resolver.getRespondingMethodDefinitions(typeStr, methodName);
      final boolean hasConcreteImpl =
          implementations.stream()
              .anyMatch(
                  methodDef ->
                      !methodDef.getModifiers().contains(MethodDefinition.Modifier.ABSTRACT));
      if (!hasConcreteImpl) {
        final String ancestorName = abstractMethod.getTypeName().getFullString();
        final String typeName = typeStr.getFullString();
        final String message = MESSAGE.formatted(ancestorName, methodName, typeName);
        final AstNode issueNode = this.getIssueNode(exemplarDefinition);
        this.addIssue(issueNode, message);
      }
    }
  }

  private AstNode getIssueNode(final ExemplarDefinition exemplarDefinition) {
    final AstNode definitionNode = exemplarDefinition.getNode();
    final AstNode argumentsNode = definitionNode.getFirstDescendant(MagikGrammar.ARGUMENTS);
    if (argumentsNode == null) {
      return definitionNode;
    }

    final ArgumentsNodeHelper helper = new ArgumentsNodeHelper(argumentsNode);
    final AstNode argumentNode = helper.getArgument(0);
    return argumentNode != null ? argumentNode : definitionNode;
  }
}
