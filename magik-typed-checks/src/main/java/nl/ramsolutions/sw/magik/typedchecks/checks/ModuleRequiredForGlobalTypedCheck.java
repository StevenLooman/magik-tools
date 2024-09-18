package nl.ramsolutions.sw.magik.typedchecks.checks;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.RecognitionException;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.scope.GlobalScope;
import nl.ramsolutions.sw.magik.analysis.scope.Scope;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeEntry;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.reasoner.LocalTypeReasonerState;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.typedchecks.MagikTypedCheck;
import nl.ramsolutions.sw.moduledef.ModuleDefFile;
import nl.ramsolutions.sw.moduledef.ModuleDefFileScanner;
import nl.ramsolutions.sw.moduledef.ModuleDefinition;
import nl.ramsolutions.sw.moduledef.ModuleUsage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.check.Rule;

/** Check to test if the module is required for a used global. */
@Rule(key = ModuleRequiredForGlobalTypedCheck.CHECK_KEY)
public class ModuleRequiredForGlobalTypedCheck extends MagikTypedCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "ModuleRequiredForGlobal";

  private static final Logger LOGGER =
      LoggerFactory.getLogger(ModuleRequiredForGlobalTypedCheck.class);
  private static final String MESSAGE = "Module '%s' defining global '%s' is not required";

  private ModuleDefinition moduleDefinition;
  private Set<String> requiredModules;

  @Override
  protected void walkPreMagik(final AstNode node) {
    this.moduleDefinition = this.readModuleDefinition();
    this.requiredModules = this.getRequiredModules();
  }

  @CheckForNull
  private ModuleDefinition readModuleDefinition() {
    final URI uri = this.getMagikFile().getUri();
    final Path path = Path.of(uri);
    final Path moduleDefPath = ModuleDefFileScanner.getModuleDefFileForPath(path);
    if (moduleDefPath == null) {
      return null;
    }

    final ModuleDefFile moduleDefFile;
    final IDefinitionKeeper definitionKeeper = this.getDefinitionKeeper();
    try {
      // TODO: Better get this from IDefinitionKeeper, instead of reading this for every file.
      moduleDefFile = new ModuleDefFile(moduleDefPath, definitionKeeper, null);
    } catch (final RecognitionException exception) {
      LOGGER.warn("Unable to parse module.def");
      return null;
    } catch (final IOException exception) {
      LOGGER.warn("Caught exception", exception);
      return null;
    }

    return moduleDefFile.getModuleDefinition();
  }

  private Set<String> getRequiredModules() {
    if (this.moduleDefinition == null) {
      return Collections.emptySet();
    }

    final Set<String> seen = new HashSet<>();

    final IDefinitionKeeper definitionKeeper = this.getDefinitionKeeper();
    final Deque<ModuleDefinition> stack = new ArrayDeque<>();
    stack.add(this.moduleDefinition);
    while (!stack.isEmpty()) {
      final ModuleDefinition currentModuleDefinition = stack.pop();
      final String moduleName = currentModuleDefinition.getName();
      if (seen.contains(moduleName)) {
        continue;
      }

      seen.add(moduleName);

      currentModuleDefinition.getUsages().stream()
          .map(ModuleUsage::getName)
          .map(definitionKeeper::getModuleDefinitions)
          .flatMap(Collection::stream)
          .forEach(stack::push);
    }

    return seen;
  }

  @Override
  protected void walkPostMagik(final AstNode node) {
    this.moduleDefinition = null;
  }

  @Override
  protected void walkPostIdentifier(final AstNode node) {
    // Get own module + requires.
    if (this.moduleDefinition == null) {
      return;
    }

    final AstNode parent = node.getParent();
    if (!parent.is(MagikGrammar.ATOM)) {
      return;
    }

    final GlobalScope globalScope = this.getMagikFile().getGlobalScope();
    final Scope scope = globalScope.getScopeForNode(node);
    if (scope == null) {
      return;
    }

    final ScopeEntry scopeEntry = scope.getScopeEntry(node);
    if (scopeEntry == null || !scopeEntry.isType(ScopeEntry.Type.GLOBAL)) {
      return;
    }

    final LocalTypeReasonerState state = this.getTypeReasonerState();
    final ExpressionResultString result = state.getNodeType(parent);
    final TypeString typeStr = result.get(0, TypeString.UNDEFINED);
    if (typeStr.isUndefined()) {
      return;
    }

    // See if the target module is required.
    final IDefinitionKeeper definitionKeeper = this.getDefinitionKeeper();
    definitionKeeper.getExemplarDefinitions(typeStr).stream()
        .filter(def -> def.getModuleName() != null)
        .filter(def -> !this.requiredModules.contains(def.getModuleName()))
        .forEach(
            def -> {
              final String globalModuleName = def.getModuleName();
              final String typeStringStr = typeStr.getFullString();
              final String message = String.format(MESSAGE, globalModuleName, typeStringStr);
              this.addIssue(node, message);
            });
  }
}
