package nl.ramsolutions.sw.magik.languageserver.references;

import com.sonar.sslr.api.AstNode;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodUsage;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodInvocationNodeHelper;
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
    final List<Location> locations = this.referencesToMethod(definitionKeeper, methodName);
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
      final IDefinitionKeeper definitionKeeper, final String methodName) {
    LOGGER.debug("Finding references to method: {}", methodName);

    // A parsed call carries no receiver type, a declared usage the type its annotation names: match
    // on the name.
    final Predicate<MethodUsage> filterPredicate =
        usage -> methodName.equals(usage.getMethodName());

    // Find references. A call in a procedure nested in a method or in the initialiser of a global
    // is a usage of both definitions.
    final Stream<MethodUsage> methodUsages =
        definitionKeeper.getMethodDefinitions().stream()
            .flatMap(def -> def.getUsedMethods().stream());
    final Stream<MethodUsage> procedureUsages =
        definitionKeeper.getProcedureDefinitions().stream()
            .flatMap(def -> def.getUsedMethods().stream());
    final Stream<MethodUsage> globalUsages =
        definitionKeeper.getGlobalDefinitions().stream()
            .flatMap(def -> def.getUsedMethods().stream());
    return Stream.of(methodUsages, procedureUsages, globalUsages)
        .flatMap(Function.identity())
        .filter(filterPredicate::test)
        .map(MethodUsage::getLocation)
        .map(Location::validLocation)
        .distinct()
        .toList();
  }
}
