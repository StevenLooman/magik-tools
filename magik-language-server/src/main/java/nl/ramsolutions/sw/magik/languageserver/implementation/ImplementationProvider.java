package nl.ramsolutions.sw.magik.languageserver.implementation;

import com.sonar.sslr.api.AstNode;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.ITypeKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.Method;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.types.UndefinedType;
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
    final TypeString typeRef = helper.getTypeString();
    final ITypeKeeper typeKeeper = magikFile.getTypeKeeper();
    final AbstractType type = typeKeeper.getType(typeRef);
    if (type == UndefinedType.INSTANCE) {
      return Collections.emptyList();
    }

    final String methodName = helper.getMethodName();
    final boolean isAbstractMethod =
        type.getMethods(methodName).stream()
            .anyMatch(method -> method.getModifiers().contains(Method.Modifier.ABSTRACT));
    if (!isAbstractMethod) {
      return Collections.emptyList();
    }

    return typeKeeper.getTypes().stream()
        .filter(anyType -> !Objects.equals(anyType.getTypeString(), type.getTypeString()))
        .filter(anyType -> anyType.isKindOf(type))
        .flatMap(anyType -> anyType.getLocalMethods(methodName).stream())
        .map(Method::getLocation)
        .map(Location::validLocation)
        .collect(Collectors.toList());
  }
}
