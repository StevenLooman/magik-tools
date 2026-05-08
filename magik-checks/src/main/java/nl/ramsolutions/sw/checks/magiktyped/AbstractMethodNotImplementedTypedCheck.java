package nl.ramsolutions.sw.checks.magiktyped;

import com.sonar.sslr.api.AstNode;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import nl.ramsolutions.sw.checks.MagikTypedCheck;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeStringResolver;
import org.sonar.check.Rule;

/** Check if concrete exemplars implement all inherited abstract methods. */
@Rule(key = AbstractMethodNotImplementedTypedCheck.CHECK_KEY)
public class AbstractMethodNotImplementedTypedCheck extends MagikTypedCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "AbstractMethodNotImplemented";

  private static final String MESSAGE = "Abstract method %s.%s is not implemented by %s.";

  private final Set<TypeString> checkedTypes = new HashSet<>();

  @Override
  protected void walkPostMethodDefinition(final AstNode node) {
    final MethodDefinitionNodeHelper helper = new MethodDefinitionNodeHelper(node);
    final TypeString typeStr = helper.getTypeString();

    // Only check each exemplar once per file.
    if (this.checkedTypes.contains(typeStr)) {
      return;
    }
    this.checkedTypes.add(typeStr);

    final TypeStringResolver resolver = this.getTypeStringResolver();
    final ExemplarDefinition exemplarDef = resolver.getExemplarDefinition(typeStr);
    if (exemplarDef == null) {
      return;
    }

    // Collect all abstract methods from ancestors.
    final Collection<TypeString> ancestors = resolver.getAllAncestors(typeStr);
    for (final TypeString ancestorTypeStr : ancestors) {
      final Collection<MethodDefinition> ancestorMethods =
          resolver.getRespondingMethodDefinitions(ancestorTypeStr);
      for (final MethodDefinition ancestorMethod : ancestorMethods) {
        if (!ancestorMethod.getModifiers().contains(MethodDefinition.Modifier.ABSTRACT)) {
          continue;
        }

        final String methodName = ancestorMethod.getMethodName();

        // Check if the current type has a concrete implementation.
        final Collection<MethodDefinition> implementations =
            resolver.getRespondingMethodDefinitions(typeStr, methodName);
        final boolean hasConcreteImpl =
            implementations.stream()
                .anyMatch(
                    methodDef ->
                        !methodDef.getModifiers().contains(MethodDefinition.Modifier.ABSTRACT));
        if (!hasConcreteImpl) {
          final String ancestorName = ancestorTypeStr.getFullString();
          final String typeName = typeStr.getFullString();
          final String message = MESSAGE.formatted(ancestorName, methodName, typeName);
          this.addIssue(node, message);
        }
      }
    }
  }
}
