package nl.ramsolutions.sw.magik.languageserver.typehierarchy;

import com.sonar.sslr.api.AstNode;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeStringResolver;
import nl.ramsolutions.sw.magik.analysis.typing.reasoner.LocalTypeReasonerState;
import nl.ramsolutions.sw.magik.api.MagikGrammar;

/** Provides the exemplar definition for a typed atom. */
class AtomTypeHierarchyModule implements TypeHierarchyModule {

  private final IDefinitionKeeper definitionKeeper;

  AtomTypeHierarchyModule(final IDefinitionKeeper definitionKeeper) {
    this.definitionKeeper = definitionKeeper;
  }

  @Override
  public Optional<List<ExemplarDefinition>> tryPrepareTypeHierarchy(
      final TypeHierarchyContext context) {
    final AstNode positionNode = context.positionNode();
    final AstNode atomNode =
        AstQuery.getParentFromChain(positionNode, MagikGrammar.IDENTIFIER, MagikGrammar.ATOM);
    if (atomNode == null) {
      return Optional.empty();
    }

    final MagikTypedFile magikFile = context.file();
    final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();
    final ExpressionResultString result = reasonerState.getNodeType(atomNode);
    final TypeString typeStr = result.get(0, null);
    if (typeStr == null || typeStr.isUndefined()) {
      return Optional.of(Collections.emptyList());
    }

    final TypeStringResolver resolver = new TypeStringResolver(this.definitionKeeper);
    final ExemplarDefinition exemplarDef = resolver.getExemplarDefinition(typeStr);
    Objects.requireNonNull(exemplarDef);
    return Optional.of(List.of(exemplarDef));
  }
}
