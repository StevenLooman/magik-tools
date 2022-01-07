package nl.ramsolutions.sw.magik.languageserver.hover;

import com.sonar.sslr.api.AstNode;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.analysis.Range;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodInvocationNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.ITypeKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.LocalTypeReasoner;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResult;
import nl.ramsolutions.sw.magik.analysis.typing.types.GlobalReference;
import nl.ramsolutions.sw.magik.analysis.typing.types.Method;
import nl.ramsolutions.sw.magik.analysis.typing.types.SelfType;
import nl.ramsolutions.sw.magik.analysis.typing.types.UndefinedType;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.languageserver.Lsp4jConversion;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.MarkupKind;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.ServerCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hover provider.
 */
public class HoverProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(HoverProvider.class);

    private static final String SECTION_END = "\n\n---\n\n";

    /**
     * Set server capabilities.
     * @param capabilities Server capabilities.
     */
    public void setCapabilities(final ServerCapabilities capabilities) {
        capabilities.setHoverProvider(true);
    }

    /**
     * Provide a hover at the given position.
     * @param magikFile Magik file.
     * @param position Position in file.
     * @return Hover at position.
     */
    public Hover provideHover(final MagikTypedFile magikFile, final Position position) {
        // Parse and reason magik.
        final AstNode node = magikFile.getTopNode();
        final AstNode hoveredTokenNode = AstQuery.nodeAt(node, Lsp4jConversion.positionFromLsp4j(position));
        if (hoveredTokenNode == null) {
            return null;
        }
        final AstNode hoveredNode = hoveredTokenNode.getParent();
        LOGGER.debug("Hovering on: {}", hoveredNode.getTokenValue());

        // See what we should provide a hover for.
        final StringBuilder builder = new StringBuilder();
        if (hoveredNode.is(MagikGrammar.IDENTIFIER)
            && hoveredNode.getParent().is(MagikGrammar.METHOD_DEFINITION)) {
            // Method definition (exemplar or method name).
            this.provideHoverMethodDefinition(magikFile, hoveredNode, builder);
        } else if (hoveredNode.is(MagikGrammar.IDENTIFIER)
                   && hoveredNode.getParent().is(MagikGrammar.METHOD_INVOCATION)) {
            this.provideHoverMethodInvocation(magikFile, hoveredNode, builder);
        } else if (hoveredNode.getParent().is(MagikGrammar.ATOM)
                   || hoveredNode.getParent().is(MagikGrammar.SLOT)) {
            final AstNode atomNode = hoveredNode.getFirstAncestor(MagikGrammar.ATOM);
            this.provideHoverAtom(magikFile, atomNode, builder);
        } else if (hoveredNode.getParent().is(MagikGrammar.PARAMETER)) {
            final AstNode parameterNode = hoveredNode.getParent();
            this.provideHoverAtom(magikFile, parameterNode, builder);
        }

        final MarkupContent contents = new MarkupContent(MarkupKind.MARKDOWN, builder.toString());
        final Range range = new Range(hoveredTokenNode);
        final org.eclipse.lsp4j.Range rangeLsp4j = Lsp4jConversion.rangeToLsp4j(range);
        return new Hover(contents, rangeLsp4j);
    }

    /**
     * Provide hover for an atom.
     * @param magikFile Magik file.
     * @param atomNode Atom node hovered on.
     * @param builder Builder to add text to.
     */
    private void provideHoverAtom(final MagikTypedFile magikFile, final AstNode atomNode, final StringBuilder builder) {
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();
        final ExpressionResult result = reasoner.getNodeTypeSilent(atomNode);
        if (result != null) {
            LOGGER.debug("Providing hover for node: {}", atomNode.getTokenValue());
            final ITypeKeeper typeKeeper = magikFile.getTypeKeeper();
            this.buildTypeDoc(typeKeeper, reasoner, atomNode, builder);
        }
    }

    /**
     * Provide hover for a method invocation.
     * @param magikFile Magik file.
     * @param hoveredNode Hovered node.
     * @param builder Builder to add text to.
     */
    private void provideHoverMethodInvocation(
            final MagikTypedFile magikFile,
            final AstNode hoveredNode,
            final StringBuilder builder) {
        final AstNode providingNode = hoveredNode.getParent();
        final AstNode previousSiblingNode = providingNode.getPreviousSibling();
        if (previousSiblingNode != null) {
            final MethodInvocationNodeHelper helper = new MethodInvocationNodeHelper(providingNode);
            final String methodName = helper.getMethodName();
            LOGGER.debug("Providing hover for node: {}, method: {}",
                    previousSiblingNode.getTokenValue(), methodName);
            if (methodName != null) {
                final ITypeKeeper typeKeeper = magikFile.getTypeKeeper();
                final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();
                this.buildMethodDoc(typeKeeper, reasoner, previousSiblingNode, methodName, builder);
            }
        }
    }

    /**
     * Provide hover for a method definition. Either the exemplar or the method name.
     * @param magikFile Magik file.
     * @param hoveredNode Hovered node.
     * @param builder Builder to add text to.
     */
    private void provideHoverMethodDefinition(
            final MagikTypedFile magikFile,
            final AstNode hoveredNode,
            final StringBuilder builder) {
        final ITypeKeeper typeKeeper = magikFile.getTypeKeeper();
        final AstNode methodDefNode = hoveredNode.getParent();
        final MethodDefinitionNodeHelper methodDefHelper = new MethodDefinitionNodeHelper(methodDefNode);
        final List<AstNode> identifierNodes = methodDefNode.getChildren(MagikGrammar.IDENTIFIER);
        if (hoveredNode == identifierNodes.get(0)) {
            // Hovered over exemplar.
            final GlobalReference globalRef = methodDefHelper.getTypeGlobalReference();
            LOGGER.debug("Providing hover for type: {}", globalRef);
            this.buildTypeDoc(typeKeeper, globalRef, builder);
        } else if (hoveredNode == identifierNodes.get(1)) {
            // Hovered over method name.
            final GlobalReference globalRef = methodDefHelper.getTypeGlobalReference();
            final String methodName = methodDefHelper.getMethodName();
            LOGGER.debug("Providing hover for type: {}, method: {}", globalRef, methodName);
            this.buildMethodDoc(typeKeeper, globalRef, methodName, builder);
        }
    }

    /**
     * Build hover text for type doc.
     * @param typeKeeper TypeKeeper to use.
     * @param globalReference Global reference to type.
     * @param builder {{StringBuilder}} to fill.
     */
    private void buildTypeDoc(
            final ITypeKeeper typeKeeper,
            final GlobalReference globalReference,
            final StringBuilder builder) {
        final AbstractType type = typeKeeper.getType(globalReference);
        this.buildTypeSignatureDoc(type, builder);
    }

    /**
     * Build hover text for type doc.
     * @param typeKeeper TypeKeeper.
     * @param reasoner {{LocalTypeReasoner}} which has reasoned over the AST.
     * @param node {{AstNode}} to get info from.
     * @param builder {{StringBuilder}} to fill.
     */
    private void buildTypeDoc(
            final ITypeKeeper typeKeeper,
            final LocalTypeReasoner reasoner,
            final AstNode node,
            final StringBuilder builder) {
        // Get type from reasoner.
        final ExpressionResult result = reasoner.getNodeType(node);

        // We know what self is.
        AbstractType type = result.get(0, UndefinedType.INSTANCE);
        if (type == SelfType.INSTANCE) {
            final AstNode methodDefNode = node.getFirstAncestor(MagikGrammar.METHOD_DEFINITION);
            if (methodDefNode == null) {
                // This can happen in case of a procedure definition calling a method on _self.
                type = UndefinedType.INSTANCE;
            } else {
                final MethodDefinitionNodeHelper methodDefHelper = new MethodDefinitionNodeHelper(methodDefNode);
                final GlobalReference globalRef = methodDefHelper.getTypeGlobalReference();
                type = typeKeeper.getType(globalRef);
            }
        }

        this.buildTypeSignatureDoc(type, builder);
    }

    /**
     * Build hover text for method doc.
     * @param typeKeeper TypeKeeper.
     * @param pakkage Name of package.
     * @param typeName Name of type.
     * @param methodName Name of method.
     * @param builder {{StringBuilder}} to fill.
     */
    private void buildMethodDoc(
            final ITypeKeeper typeKeeper,
            final GlobalReference globalReference,
            final String methodName,
            final StringBuilder builder) {
        final AbstractType type = typeKeeper.getType(globalReference);

        // Get method info.
        final Collection<Method> methods = type.getMethods(methodName);
        if (methods.isEmpty()) {
            this.buildMethodUnknownDoc(type, methodName, builder);
            return;
        }

        methods.forEach(method -> this.buildMethodSignatureDoc(method, builder));
    }

    /**
     * Build hover text for method doc.
     * @param typeKeeper TypeKeeper.
     * @param reasoner LocalTypeReasoner.
     * @param node AstNode, METHOD_INVOCATION.
     * @param methodName Name of invoked method.
     * @param builder {{StringBuilder}} to fill.
     */
    private void buildMethodDoc(
            final ITypeKeeper typeKeeper,
            final LocalTypeReasoner reasoner,
            final AstNode node,
            final String methodName,
            final StringBuilder builder) {
        // Get type from reasoner.
        final ExpressionResult result = reasoner.getNodeType(node);

        // We know what self is.
        AbstractType type = result.get(0, UndefinedType.INSTANCE);
        if (type == SelfType.INSTANCE) {
            final AstNode methodDefNode = node.getFirstAncestor(MagikGrammar.METHOD_DEFINITION);
            if (methodDefNode == null) {
                // This can happen in case of a procedure definition calling a method on _self.
                type = UndefinedType.INSTANCE;
            } else {
                final MethodDefinitionNodeHelper helper = new MethodDefinitionNodeHelper(methodDefNode);
                final GlobalReference globalRef = helper.getTypeGlobalReference();
                type = typeKeeper.getType(globalRef);
            }
        }

        // Get method info.
        final Collection<Method> methods = type.getMethods(methodName);
        if (methods.isEmpty()) {
            this.buildMethodUnknownDoc(type, methodName, builder);
            return;
        }

        methods.forEach(method -> this.buildMethodSignatureDoc(method, builder));
    }

    private void buildInheritanceDoc(final AbstractType type, final StringBuilder builder, final int indent) {
        final String indentStr = "&nbsp;&nbsp;".repeat(indent);
        type.getParents().forEach(parentType -> {
            builder
                .append(indentStr)
                .append(" ↳ ")
                .append(parentType.getFullName())
                .append("\n\n");

            this.buildInheritanceDoc(parentType, builder, indent + 1);
        });
        if (indent == 0) {
            builder.append(SECTION_END);
        }
    }

    private void buildMethodUnknownDoc(final AbstractType type, final String methodName, final StringBuilder builder) {
        builder
            .append("Unknown method ")
            .append(methodName)
            .append(" on type ")
            .append(type.getFullName())
            .append("\n")
            .append(SECTION_END);
    }

    private void buildMethodSignatureDoc(final Method method, final StringBuilder builder) {
        // Method signature.
        builder
            .append("## ")
            .append(method.getSignature())
            .append("\n")
            .append(" → ")
            .append(method.getCallResult().getTypeNames(", "))
            .append(SECTION_END);

        // Method doc.
        final String methodDoc = method.getDoc();
        if (methodDoc != null) {
            final String methodDocMd = methodDoc.lines()
                .map(String::trim)
                .collect(Collectors.joining("\n\n"));
            builder
                .append(methodDocMd)
                .append(SECTION_END);
        }
    }

    private void buildTypeSignatureDoc(final AbstractType type, final StringBuilder builder) {
        // Type signature.
        builder
            .append("## ")
            .append(type.getFullName())
            .append(SECTION_END);

        // Inheritance.
        this.buildInheritanceDoc(type, builder, 0);

        // Type doc.
        final String typeDoc = type.getDoc();
        if (typeDoc != null) {
            final String typeDocMd = typeDoc.lines()
                .map(String::trim)
                .collect(Collectors.joining("\n\n"));
            builder
                .append(typeDocMd)
                .append(SECTION_END);
        }
    }

}
