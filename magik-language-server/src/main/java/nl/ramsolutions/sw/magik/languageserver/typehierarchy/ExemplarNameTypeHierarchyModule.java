package nl.ramsolutions.sw.magik.languageserver.typehierarchy;

import com.sonar.sslr.api.AstNode;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeStringResolver;
import nl.ramsolutions.sw.magik.api.MagikGrammar;

/** Provides the exemplar definition for a method definition's exemplar name. */
class ExemplarNameTypeHierarchyModule implements TypeHierarchyModule {

  private final IDefinitionKeeper definitionKeeper;

  ExemplarNameTypeHierarchyModule(final IDefinitionKeeper definitionKeeper) {
    this.definitionKeeper = definitionKeeper;
  }

  @Override
  public Optional<List<ExemplarDefinition>> tryPrepareTypeHierarchy(
      final TypeHierarchyContext context) {
    final AstNode positionNode = context.positionNode();
    final AstNode methodDefinitionNode =
        AstQuery.getParentFromChain(
            positionNode,
            MagikGrammar.IDENTIFIER,
            MagikGrammar.EXEMPLAR_NAME,
            MagikGrammar.METHOD_DEFINITION);
    if (methodDefinitionNode == null) {
      return Optional.empty();
    }

    final MethodDefinitionNodeHelper methodDefinitionNodeHelper =
        new MethodDefinitionNodeHelper(methodDefinitionNode);
    final TypeString typeStr = methodDefinitionNodeHelper.getExemplarTypeString();
    final TypeStringResolver resolver = new TypeStringResolver(this.definitionKeeper);
    final ExemplarDefinition exemplarDef = resolver.getExemplarDefinition(typeStr);
    if (exemplarDef == null) {
      return Optional.of(Collections.emptyList());
    }

    return Optional.of(List.of(exemplarDef));
  }
}
