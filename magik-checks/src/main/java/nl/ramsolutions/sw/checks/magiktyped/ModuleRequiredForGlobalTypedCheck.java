package nl.ramsolutions.sw.checks.magiktyped;

import com.sonar.sslr.api.AstNode;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import nl.ramsolutions.sw.checks.MagikTypedCheck;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.scope.GlobalScope;
import nl.ramsolutions.sw.magik.analysis.scope.Scope;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeEntry;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.reasoner.LocalTypeReasonerState;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.moduledef.ModuleDefFile;
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
  private static final AtomicBoolean LOADED_MODULES_UNKNOWN_LOGGED = new AtomicBoolean();

  private ModuleDefinition moduleDefinition;
  private Set<String> requiredModules;
  private Set<String> loadedModules = Collections.emptySet();

  @Override
  protected void walkPreMagik(final AstNode node) {
    this.moduleDefinition = this.readModuleDefinition();
    this.loadedModules = Collections.emptySet();
    if (this.moduleDefinition != null) {
      final IDefinitionKeeper definitionKeeper = this.getDefinitionKeeper();
      this.loadedModules = definitionKeeper.getLoadedModuleNames();
      if (this.loadedModules.isEmpty()) {
        this.logLoadedModulesUnknownOnce();
      }
    }
    this.requiredModules = this.getRequiredModules();
  }

  private void logLoadedModulesUnknownOnce() {
    if (LOADED_MODULES_UNKNOWN_LOGGED.compareAndSet(false, true)) {
      LOGGER.warn(
          "ModuleRequiredForGlobal has no always-loaded module record: either no types database "
              + "is configured, or the configured database predates sw_type_dumper's "
              + "loaded-module record. The check is reporting nothing until a types database "
              + "carrying a loaded-module record is configured; re-dump with the current "
              + "sw_type_dumper to restore it.");
    }
  }

  @CheckForNull
  private ModuleDefinition readModuleDefinition() {
    final ModuleDefFile moduleDefFile = this.getMagikFile().getModuleDefFile();
    if (moduleDefFile == null) {
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

      Stream.concat(
              currentModuleDefinition.getRequiredModules().stream(),
              currentModuleDefinition.getTestModules().stream())
          .map(ModuleUsage::getName)
          .map(definitionKeeper::getModuleDefinitions)
          .flatMap(Collection::stream)
          .forEach(stack::push);
    }

    seen.addAll(this.loadedModules);

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

    if (this.loadedModules.isEmpty()) {
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
              final String message = MESSAGE.formatted(globalModuleName, typeStringStr);
              this.addIssue(node, message);
            });
  }
}
