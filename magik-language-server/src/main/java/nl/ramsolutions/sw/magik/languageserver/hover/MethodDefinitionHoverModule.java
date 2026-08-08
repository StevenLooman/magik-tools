package nl.ramsolutions.sw.magik.languageserver.hover;

import com.sonar.sslr.api.AstNode;
import java.util.Optional;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeStringResolver;
import nl.ramsolutions.sw.magik.analysis.typing.reasoner.LocalTypeReasonerState;
import nl.ramsolutions.sw.magik.api.MagikGrammar;

/** Provides hover for the method name of a method definition. */
public class MethodDefinitionHoverModule implements HoverModule<MagikTypedFile> {

  @Override
  public Optional<String> tryHover(final HoverContext<MagikTypedFile> context) {
    final AstNode hoveredNode = context.getHoveredNode();
    final AstNode parentNode = hoveredNode.getParent();
    if (parentNode == null
        || !parentNode.is(MagikGrammar.METHOD_NAME)
        || !AstQuery.parentIs(parentNode, MagikGrammar.METHOD_DEFINITION)) {
      return Optional.empty();
    }

    final MagikTypedFile magikFile = context.file();
    final AstNode methodDefNode = hoveredNode.getFirstAncestor(MagikGrammar.METHOD_DEFINITION);
    final AstNode exemplarNameNode = methodDefNode.getFirstChild(MagikGrammar.EXEMPLAR_NAME);
    final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();
    final ExpressionResultString result = reasonerState.getNodeType(exemplarNameNode);
    final TypeString typeStr = result.get(0, TypeString.UNDEFINED);

    final MethodDefinitionNodeHelper methodDefHelper =
        new MethodDefinitionNodeHelper(methodDefNode);
    final String methodName = methodDefHelper.getMethodName();
    final TypeStringResolver resolver = magikFile.getTypeStringResolver();
    final StringBuilder builder = new StringBuilder();
    resolver
        .getRespondingMethodDefinitions(typeStr, methodName)
        .forEach(methodDef -> HoverDocBuilder.buildMethodSignatureDoc(methodDef, builder));
    return Optional.of(builder.toString());
  }
}
