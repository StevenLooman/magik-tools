package nl.ramsolutions.sw.magik.languageserver.definitions;

import com.sonar.sslr.api.AstNode;
import java.util.List;
import java.util.Optional;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodInvocationNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.SelfHelper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeStringResolver;
import nl.ramsolutions.sw.magik.analysis.typing.reasoner.LocalTypeReasonerState;
import nl.ramsolutions.sw.magik.api.MagikGrammar;

/** Provides definitions for a method invocation's method name. */
public class MethodInvocationDefinitionModule implements DefinitionModule<MagikTypedFile> {

  @Override
  public Optional<List<Location>> tryDefinitions(final DefinitionContext<MagikTypedFile> context) {
    final AstNode positionNode = context.positionNode();
    final AstNode wantedNode = DefinitionAncestor.nearest(positionNode);
    if (wantedNode == null || !wantedNode.is(MagikGrammar.METHOD_INVOCATION)) {
      return Optional.empty();
    }

    final MethodInvocationNodeHelper invocationHelper = new MethodInvocationNodeHelper(wantedNode);
    final String methodName = invocationHelper.getMethodName();

    final AstNode previousSiblingNode = wantedNode.getPreviousSibling();
    final MagikTypedFile magikFile = context.file();
    final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();
    final ExpressionResultString result = reasonerState.getNodeType(previousSiblingNode);
    final TypeString resultTypeStr = result.get(0, TypeString.UNDEFINED);
    final TypeString typeStr = SelfHelper.substituteSelf(resultTypeStr, wantedNode);

    final TypeStringResolver resolver = magikFile.getTypeStringResolver();
    final List<Location> locations =
        resolver.getRespondingMethodDefinitions(typeStr, methodName).stream()
            .map(MethodDefinition::getLocation)
            .map(Location::validLocation)
            .toList();
    return Optional.of(locations);
  }
}
