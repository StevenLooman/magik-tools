package nl.ramsolutions.sw.checks;

import com.sonar.sslr.api.AstNode;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.Collection;
import java.util.List;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.ITypeStringDefinition;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.PackageNodeHelper;
import nl.ramsolutions.sw.magik.analysis.scope.GlobalScope;
import nl.ramsolutions.sw.magik.analysis.scope.Scope;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeEntry;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeStringResolver;
import nl.ramsolutions.sw.magik.analysis.typing.reasoner.LocalTypeReasoner;
import nl.ramsolutions.sw.magik.analysis.typing.reasoner.LocalTypeReasonerState;
import nl.ramsolutions.sw.magik.api.MagikGrammar;

/** Magik typed check. */
public class MagikTypedCheck extends MagikCheck {

  /**
   * Get the {@link MagikTypedFile}.
   *
   * @return Magik typed file.
   */
  protected MagikTypedFile getMagikTypedFile() {
    final MagikFile magikFile = this.getMagikFile();
    if (!(magikFile instanceof MagikTypedFile)) {
      throw new IllegalStateException();
    }

    return (MagikTypedFile) magikFile;
  }

  /**
   * Get {@link IDefinitionKeeper}.
   *
   * @return Definition keeper.
   */
  protected IDefinitionKeeper getDefinitionKeeper() {
    final MagikTypedFile magikTypedFile = this.getMagikTypedFile();
    return magikTypedFile.getDefinitionKeeper();
  }

  /**
   * Get the {@link TypeStringResolver}.
   *
   * @return The {@link TypeStringResolver}.
   */
  public TypeStringResolver getTypeStringResolver() {
    final MagikTypedFile magikTypedFile = this.getMagikTypedFile();
    return magikTypedFile.getTypeStringResolver();
  }

  /**
   * Get the resulting state from the {@link LocalTypeReasoner}.
   *
   * @return The {@link LocalTypeReasonerState}.
   */
  public synchronized LocalTypeReasonerState getTypeReasonerState() {
    final MagikTypedFile magikTypedFile = this.getMagikTypedFile();
    return magikTypedFile.getTypeReasonerState();
  }

  /**
   * Get type method invoked on.
   *
   * @param node METHOD_INVOCATION node.
   * @return Type method is invoked, or UNDEFINED_TYPE.
   */
  protected TypeString getTypeInvokedOn(final AstNode node) {
    if (node.isNot(MagikGrammar.METHOD_INVOCATION)) {
      throw new IllegalStateException();
    }

    final AstNode previousSibling = node.getPreviousSibling();
    final LocalTypeReasonerState reasonerState = this.getTypeReasonerState();
    final ExpressionResultString result = reasonerState.getNodeType(previousSibling);
    final TypeString typeStr = result.get(0, TypeString.UNDEFINED);
    if (typeStr.equals(TypeString.SELF)) {
      final AstNode methodDefNode = node.getFirstAncestor(MagikGrammar.METHOD_DEFINITION);
      return this.getTypeOfMethodDefinition(methodDefNode);
    }

    return typeStr;
  }

  /**
   * Get type method invoked on, without resolving self.
   *
   * @param node METHOD_INVOCATION node.
   * @return Type method is invoked, or UNDEFINED_TYPE.
   */
  protected TypeString getTypeInvokedOnWithSelf(final AstNode node) {
    if (node.isNot(MagikGrammar.METHOD_INVOCATION)) {
      throw new IllegalStateException();
    }

    final AstNode previousSibling = node.getPreviousSibling();
    final LocalTypeReasonerState reasonerState = this.getTypeReasonerState();
    final ExpressionResultString result = reasonerState.getNodeType(previousSibling);
    return result.get(0, TypeString.UNDEFINED);
  }

  /**
   * Get type of method definition.
   *
   * @param node METHOD_DEFINITION node.
   * @return The type of the method definition.
   */
  protected TypeString getTypeOfMethodDefinition(final @Nullable AstNode node) {
    if (node == null) {
      return TypeString.UNDEFINED;
    }

    final MethodDefinitionNodeHelper methodDefHelper = new MethodDefinitionNodeHelper(node);
    return methodDefHelper.getExemplarTypeString();
  }

  /**
   * Get the definitions a global reference names: the exemplars, procedures and globals its
   * identifier resolves to, not the type the reference evaluates to.
   *
   * @param node IDENTIFIER node.
   * @return The definitions, empty when the identifier is not a global reference.
   */
  protected Collection<ITypeStringDefinition> getGlobalReferenceDefinitions(final AstNode node) {
    final AstNode parent = node.getParent();
    if (!parent.is(MagikGrammar.ATOM) || !this.isGlobalReference(node)) {
      return List.of();
    }

    final PackageNodeHelper helper = new PackageNodeHelper(node);
    final String currentPackage = helper.getCurrentPackage();
    final String identifier = node.getTokenValue();
    final TypeString typeStr = TypeString.ofIdentifier(identifier, currentPackage);
    final TypeStringResolver resolver = this.getTypeStringResolver();
    return resolver.resolve(typeStr);
  }

  private boolean isGlobalReference(final AstNode node) {
    final MagikFile magikFile = this.getMagikFile();
    final GlobalScope globalScope = magikFile.getGlobalScope();
    final Scope scope = globalScope.getScopeForNode(node);
    if (scope == null) {
      return false;
    }

    final ScopeEntry scopeEntry = scope.getScopeEntry(node);
    return scopeEntry != null
        && (scopeEntry.isType(ScopeEntry.Type.GLOBAL)
            || scopeEntry.isType(ScopeEntry.Type.DYNAMIC));
  }
}
