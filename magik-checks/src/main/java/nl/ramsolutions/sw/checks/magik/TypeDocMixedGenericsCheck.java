package nl.ramsolutions.sw.checks.magik;

import com.sonar.sslr.api.AstNode;
import java.util.List;
import java.util.Map;
import nl.ramsolutions.sw.checks.MagikCheck;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.parser.TypeDocParser;
import org.sonar.check.Rule;

/** Check for mixed generic definitions and references in type annotations. */
@Rule(key = TypeDocMixedGenericsCheck.CHECK_KEY)
public class TypeDocMixedGenericsCheck extends MagikCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "TypeDocMixedGenerics";

  private static final String MESSAGE =
      "Type annotation contains mixed generic definitions and references: %s.";

  @Override
  protected void walkPostMethodDefinition(final AstNode node) {
    this.checkTypeAnnotations(node);
  }

  @Override
  protected void walkPostProcedureDefinition(final AstNode node) {
    this.checkTypeAnnotations(node);
  }

  private void checkTypeAnnotations(final AstNode node) {
    final TypeDocParser typeDocParser = new TypeDocParser(node);
    this.checkTypeNodes(typeDocParser.getParameterTypeNodes());
    this.checkTypeNodes(typeDocParser.getReturnTypeNodes());
    this.checkTypeNodes(typeDocParser.getLoopTypeNodes());
    this.checkTypeNodes(typeDocParser.getSlotTypeNodes());
    this.checkTypeNodes(typeDocParser.getGenericTypeNodes());
  }

  private void checkTypeNodes(final Map<AstNode, TypeString> typeNodes) {
    typeNodes.forEach(
        (astNode, typeString) -> {
          this.checkTypeString(astNode, typeString);
        });
  }

  private void checkTypeString(final AstNode node, final TypeString typeString) {
    if (typeString.isVariadic()) {
      this.checkTypeString(node, typeString.getVariadicInner());
      return;
    }

    if (typeString.isCombined()) {
      typeString
          .getCombinedTypes()
          .forEach(combinedType -> this.checkTypeString(node, combinedType));
      return;
    }

    final List<TypeString> generics = typeString.getGenerics();
    if (generics.size() < 2) {
      return;
    }

    final boolean hasDefinitions = generics.stream().anyMatch(TypeString::isGenericDefinition);
    final boolean hasReferences = generics.stream().anyMatch(TypeString::isGenericReference);
    if (hasDefinitions && hasReferences) {
      final String message = MESSAGE.formatted(typeString.getFullString());
      this.addIssue(node, message);
    }

    // Recursively check generic type values.
    generics.stream()
        .filter(TypeString::isGenericDefinition)
        .forEach(gen -> this.checkTypeString(node, gen.getGenericType()));
  }
}
