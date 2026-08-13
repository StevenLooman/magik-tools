package nl.ramsolutions.sw.magik.languageserver.references;

import com.sonar.sslr.api.AstNode;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodUsage;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodInvocationNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Provides references to a method, from either its definition or an invocation of it. */
public class MethodNameReferencesModule implements ReferencesModule<MagikTypedFile> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodNameReferencesModule.class);

  @Override
  public Optional<List<Location>> tryReferences(final ReferencesContext<MagikTypedFile> context) {
    final AstNode positionNode = context.positionNode();
    final AstNode wantedNode = ReferencesAncestor.nearest(positionNode);
    if (wantedNode == null || !wantedNode.is(MagikGrammar.METHOD_NAME)) {
      return Optional.empty();
    }

    final String methodName = this.getMethodName(wantedNode);
    if (methodName == null) {
      return Optional.of(Collections.emptyList());
    }

    final MagikTypedFile magikFile = context.file();
    final IDefinitionKeeper definitionKeeper = magikFile.getDefinitionKeeper();
    final List<Location> locations =
        this.referencesToMethod(definitionKeeper, TypeString.UNDEFINED, methodName);
    return Optional.of(locations);
  }

  @CheckForNull
  private String getMethodName(final AstNode methodNameNode) {
    final AstNode parentNode = methodNameNode.getParent();
    if (parentNode.is(MagikGrammar.METHOD_DEFINITION)) {
      return new MethodDefinitionNodeHelper(parentNode).getMethodName();
    } else if (parentNode.is(MagikGrammar.METHOD_INVOCATION)) {
      return new MethodInvocationNodeHelper(parentNode).getMethodName();
    }

    return null;
  }

  private List<Location> referencesToMethod(
      final IDefinitionKeeper definitionKeeper,
      final TypeString typeName,
      final String methodName) {
    LOGGER.debug("Finding references to method: {}", methodName);

    // Build set of types which may contain this method: type + ancestors.
    final Set<TypeString> wantedTypeRefs = new HashSet<>();
    wantedTypeRefs.add(TypeString.UNDEFINED); // For unreasoned/undetermined calls.
    wantedTypeRefs.add(typeName);
    // TODO: Add all ancestors too?

    final Collection<MethodUsage> searchedMethodUsages =
        wantedTypeRefs.stream()
            .map(wantedTypeRef -> new MethodUsage(wantedTypeRef, methodName))
            .collect(Collectors.toSet());
    final Predicate<MethodUsage> filterPredicate = searchedMethodUsages::contains;

    // Find references.
    return definitionKeeper.getMethodDefinitions().stream()
        .flatMap(def -> def.getUsedMethods().stream())
        .filter(filterPredicate::test)
        .map(MethodUsage::getLocation)
        .map(Location::validLocation)
        .toList();
  }
}
