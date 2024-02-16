package nl.ramsolutions.sw.magik.typedchecks;

import com.sonar.sslr.api.AstNode;
import edu.umd.cs.findbugs.annotations.Nullable;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.ITypeKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.ReadOnlyTypeKeeperAdapter;
import nl.ramsolutions.sw.magik.analysis.typing.reasoner.LocalTypeReasonerState;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResult;
import nl.ramsolutions.sw.magik.analysis.typing.types.SelfType;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.types.UndefinedType;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.checks.MagikCheck;

/**
 * Magik typed check.
 */
public class MagikTypedCheck extends MagikCheck {

    /**
     * Get the {@link MagikTypedFile}.
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
     * @return Definition keeper.
     */
    protected IDefinitionKeeper getDefinitionKeeper() {
        final MagikTypedFile magikTypedFile = this.getMagikTypedFile();
        return magikTypedFile.getDefinitionKeeper();
    }

    /**
     * Get {@link LocalTypeReasonerState}, after reasoning.
     * @return LocalTypeReasonerState.
     */
    protected LocalTypeReasonerState getTypeReasonerState() {
        final MagikTypedFile magikTypedFile = this.getMagikTypedFile();
        return magikTypedFile.getTypeReasonerState();
    }

    /**
     * Get {@link ITypeKeeper}.
     * @return ITypeKeeper.
     */
    protected ITypeKeeper getTypeKeeper() {
        final MagikTypedFile magikTypedFile = this.getMagikTypedFile();
        final ITypeKeeper typeKeeper = magikTypedFile.getTypeKeeper();
        return new ReadOnlyTypeKeeperAdapter(typeKeeper);
    }

    /**
     * Get type method invoked on.
     * @param node METHOD_INVOCATION node.
     * @return Type method is invoked, or UNDEFINED_TYPE.
     */
    protected AbstractType getTypeInvokedOn(final AstNode node) {
        if (node.isNot(MagikGrammar.METHOD_INVOCATION)) {
            throw new IllegalStateException();
        }

        final AstNode previousSibling = node.getPreviousSibling();
        final LocalTypeReasonerState reasonerState = this.getTypeReasonerState();
        final ExpressionResult result = reasonerState.getNodeType(previousSibling);
        final AbstractType type = result.get(0, UndefinedType.INSTANCE);
        if (type == SelfType.INSTANCE) {
            final AstNode methodDefNode = node.getFirstAncestor(MagikGrammar.METHOD_DEFINITION);
            return this.getTypeOfMethodDefinition(methodDefNode);
        }

        return type;
    }

    /**
     * Get type of method definition.
     * @param node METHOD_DEFINITION node.
     * @return
     */
    protected AbstractType getTypeOfMethodDefinition(final @Nullable AstNode node) {
        if (node == null) {
            return UndefinedType.INSTANCE;
        }

        final MethodDefinitionNodeHelper methodDefHelper = new MethodDefinitionNodeHelper(node);
        final TypeString typeString = methodDefHelper.getTypeString();
        final ITypeKeeper typeKeeper = this.getTypeKeeper();
        return typeKeeper.getType(typeString);
    }

}
