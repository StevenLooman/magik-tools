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
import nl.ramsolutions.sw.magik.analysis.typing.TypeStringResolver;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
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
   * Provide implementations for {@code position} in {@code path}.
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

    final AstNode wantedNode = currentNode.getFirstAncestor(MagikGrammar.METHOD_NAME);
    if (wantedNode == null) {
      return Collections.emptyList();
    }

    return this.implementionsForMethod(magikFile, wantedNode);
  }

  private List<Location> implementionsForMethod(
      final MagikTypedFile magikFile, final AstNode wantedNode) {
    final AstNode methodDefinitionNode = wantedNode.getParent();
    final MethodDefinitionNodeHelper helper = new MethodDefinitionNodeHelper(methodDefinitionNode);
    final TypeString typeStr = helper.getTypeString();
    final TypeStringResolver resolver = magikFile.getTypeStringResolver();
    final boolean isAbstractMethod =
        resolver.getMethodDefinitions(typeStr).stream()
            .anyMatch(
                methodDef -> methodDef.getModifiers().contains(MethodDefinition.Modifier.ABSTRACT));
    if (!isAbstractMethod) {
      return Collections.emptyList();
    }

    final IDefinitionKeeper definitionKeeper = magikFile.getDefinitionKeeper();
    return definitionKeeper.getMethodDefinitions().stream()
        .filter(methodDef -> !typeStr.equals(methodDef.getTypeName()))
        .filter(methodDef -> resolver.isKindOf(methodDef.getTypeName(), typeStr))
        .map(MethodDefinition::getLocation)
        .map(Location::validLocation)
        .toList();
  }
}
