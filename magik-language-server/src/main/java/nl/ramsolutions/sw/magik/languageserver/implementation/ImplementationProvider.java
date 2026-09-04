package nl.ramsolutions.sw.magik.languageserver.implementation;

import com.sonar.sslr.api.AstNode;
import java.util.Collections;
import java.util.List;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeStringResolver;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import org.eclipse.lsp4j.ServerCapabilities;

/** Implementation provider. */
public class ImplementationProvider {

  /**
   * Set server capabilities.
   *
   * @param capabilities Server capabilities.
   */
  public void setCapabilities(final ServerCapabilities capabilities) {
    capabilities.setImplementationProvider(true);
  }

  /**
   * Provide implementations at {@link Position} in {@link MagikTypedFile}.
   *
   * @param magikFile Magik file.
   * @param position Location in file.
   * @return List of Locations for implementation.
   */
  public List<Location> provideImplementations(
      final MagikTypedFile magikFile, final Position position) {
    final AstNode node = magikFile.getTopNode();
    final AstNode currentNode = AstQuery.nodeAt(node, position, MagikGrammar.IDENTIFIER);
    if (currentNode == null) {
      return Collections.emptyList();
    }

    final AstNode methodNameNode = currentNode.getFirstAncestor(MagikGrammar.METHOD_NAME);
    if (methodNameNode == null) {
      return Collections.emptyList();
    }

    // `METHOD_NAME` also occurs in a `METHOD_INVOCATION`; only a definition is answerable here.
    final AstNode parentNode = methodNameNode.getParent();
    if (parentNode == null || !parentNode.is(MagikGrammar.METHOD_DEFINITION)) {
      return Collections.emptyList();
    }

    return this.implementationsForMethod(magikFile, parentNode);
  }

  private List<Location> implementationsForMethod(
      final MagikTypedFile magikFile, final AstNode methodDefinitionNode) {
    final MethodDefinitionNodeHelper helper = new MethodDefinitionNodeHelper(methodDefinitionNode);
    final boolean isAbstractMethod = helper.isAbstractMethod();
    if (!isAbstractMethod) {
      return Collections.emptyList();
    }

    final TypeString typeStr = helper.getExemplarTypeString();
    final String methodName = helper.getMethodName();
    final TypeStringResolver resolver = magikFile.getTypeStringResolver();
    final IDefinitionKeeper definitionKeeper = magikFile.getDefinitionKeeper();
    return definitionKeeper.getMethodDefinitions().stream()
        .filter(methodDef -> methodName.equals(methodDef.getMethodName()))
        .filter(methodDef -> !typeStr.equals(methodDef.getTypeName()))
        .filter(methodDef -> resolver.isKindOf(methodDef.getTypeName(), typeStr))
        .map(MethodDefinition::getLocation)
        .map(Location::validLocation)
        .toList();
  }
}
