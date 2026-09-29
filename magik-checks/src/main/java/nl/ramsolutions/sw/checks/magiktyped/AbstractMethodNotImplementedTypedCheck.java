package nl.ramsolutions.sw.checks.magiktyped;

import com.sonar.sslr.api.AstNode;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import nl.ramsolutions.sw.checks.MagikTypedCheck;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
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

    // Collect all abstract methods declared by ancestors, keyed by name so the same method
    // reachable via multiple ancestors is only reported once. Each ancestor's own methods are
    // read, as its responding set may resolve a name to a concrete method further up.
    final Map<String, MethodDefinition> abstractMethods = new HashMap<>();
    for (final TypeString ancestorTypeStr : resolver.getAllAncestors(typeStr)) {
      for (final MethodDefinition ancestorMethod :
          this.getOwnMethodDefinitions(resolver, ancestorTypeStr)) {
        if (ancestorMethod.getModifiers().contains(MethodDefinition.Modifier.ABSTRACT)) {
          abstractMethods.putIfAbsent(ancestorMethod.getMethodName(), ancestorMethod);
        }
      }
    }

    for (final MethodDefinition abstractMethod : abstractMethods.values()) {
      final String methodName = abstractMethod.getMethodName();

      final boolean implemented = this.isImplemented(resolver, typeStr, methodName);
      if (!implemented) {
        final String ancestorName = abstractMethod.getTypeName().getFullString();
        final String typeName = typeStr.getFullString();
        final String message = MESSAGE.formatted(ancestorName, methodName, typeName);
        final AstNode issueNode = this.getIssueNode(exemplarDefinition);
        this.addIssue(issueNode, message);
      }
    }
  }

  /**
   * Whether the type has a concrete implementation, or defines the method itself: a type declaring
   * it abstract again defers it to its own descendants.
   */
  private boolean isImplemented(
      final TypeStringResolver resolver, final TypeString typeStr, final String methodName) {
    final Collection<MethodDefinition> implementations =
        resolver.getRespondingMethodDefinitions(typeStr, methodName);
    final TypeString resolvedTypeStr =
        AbstractMethodNotImplementedTypedCheck.resolve(resolver, typeStr);
    final TypeString bareTypeStr = resolvedTypeStr.getWithoutGenerics();
    return implementations.stream()
        .anyMatch(
            methodDef -> {
              final Set<MethodDefinition.Modifier> modifiers = methodDef.getModifiers();
              final boolean isAbstract = modifiers.contains(MethodDefinition.Modifier.ABSTRACT);
              final TypeString ownerTypeStr = methodDef.getTypeName();
              final TypeString bareOwnerTypeStr = ownerTypeStr.getWithoutGenerics();
              return !isAbstract || bareOwnerTypeStr.equals(bareTypeStr);
            });
  }

  private Collection<MethodDefinition> getOwnMethodDefinitions(
      final TypeStringResolver resolver, final TypeString typeStr) {
    final TypeString resolvedTypeStr =
        AbstractMethodNotImplementedTypedCheck.resolve(resolver, typeStr);
    final IDefinitionKeeper definitionKeeper = this.getDefinitionKeeper();
    return definitionKeeper.getMethodDefinitions(resolvedTypeStr);
  }

  private static TypeString resolve(final TypeStringResolver resolver, final TypeString typeStr) {
    final ExemplarDefinition exemplarDefinition = resolver.getExemplarDefinition(typeStr);
    return exemplarDefinition != null ? exemplarDefinition.getTypeString() : typeStr;
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
