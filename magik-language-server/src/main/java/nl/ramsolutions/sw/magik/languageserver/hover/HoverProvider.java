package nl.ramsolutions.sw.magik.languageserver.hover;

import com.sonar.sslr.api.AstNode;
import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodInvocationNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.ITypeKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.reasoner.LocalTypeReasonerState;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.AliasType;
import nl.ramsolutions.sw.magik.analysis.typing.types.CombinedType;
import nl.ramsolutions.sw.magik.analysis.typing.types.Condition;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResult;
import nl.ramsolutions.sw.magik.analysis.typing.types.GenericDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.types.Method;
import nl.ramsolutions.sw.magik.analysis.typing.types.Package;
import nl.ramsolutions.sw.magik.analysis.typing.types.ProcedureInstance;
import nl.ramsolutions.sw.magik.analysis.typing.types.SelfType;
import nl.ramsolutions.sw.magik.analysis.typing.types.Slot;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
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
    @SuppressWarnings("java:S3776")
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
        final AstNode parentNode = hoveredNode.getParent();
        final AstNode parentParentNode = parentNode != null ? parentNode.getParent() : null;
        final AstNode parentNextSibling = parentNode != null ? parentNode.getNextSibling() : null;
        final StringBuilder builder = new StringBuilder();
        if (hoveredNode.is(MagikGrammar.PACKAGE_IDENTIFIER)) {
            this.provideHoverPackage(magikFile, hoveredNode, builder);
        } else if (parentNode != null) {
            if (parentNode.is(MagikGrammar.EXEMPLAR_NAME)) {
                this.provideHoverAtom(magikFile, parentNode, builder);
            } else if (parentNode.is(MagikGrammar.METHOD_NAME)) {
                this.provideHoverMethodDefinition(magikFile, hoveredNode, builder);
            } else if (hoveredNode.is(MagikGrammar.IDENTIFIER)
                    && parentNextSibling != null
                    && parentNextSibling.is(MagikGrammar.PROCEDURE_INVOCATION)) {
                this.provideHoverProcedureInvocation(magikFile, hoveredNode, builder);
            } else if (hoveredNode.is(MagikGrammar.IDENTIFIER)
                       && parentNode.is(MagikGrammar.METHOD_INVOCATION)) {
                this.provideHoverMethodInvocation(magikFile, hoveredNode, builder);
            } else if (parentNode.is(MagikGrammar.ATOM) || parentNode.is(MagikGrammar.SLOT)) {
                final AstNode atomNode = hoveredNode.getFirstAncestor(MagikGrammar.ATOM);
                this.provideHoverAtom(magikFile, atomNode, builder);
            } else if (parentNode.is(MagikGrammar.PARAMETER)) {
                final AstNode parameterNode = hoveredNode.getParent();
                this.provideHoverAtom(magikFile, parameterNode, builder);
            } else if (parentNode.is(MagikGrammar.VARIABLE_DEFINITION)) {
                this.provideHoverAtom(magikFile, hoveredNode, builder);
            } else if (parentParentNode != null && parentParentNode.is(MagikGrammar.FOR_VARIABLES)) {
                this.provideHoverAtom(magikFile, hoveredNode, builder);
            } else if (parentNode.is(MagikGrammar.EXPRESSION)) {
                final AstNode expressionNode = hoveredNode.getParent();
                this.provideHoverExpression(magikFile, expressionNode, builder);
            } else if (parentNode.is(MagikGrammar.CONDITION_NAME)) {
                this.provideHoverCondition(magikFile, hoveredNode, builder);
            }
        }

        final MarkupContent contents = new MarkupContent(MarkupKind.MARKDOWN, builder.toString());
        final Range range = new Range(hoveredTokenNode);
        final org.eclipse.lsp4j.Range rangeLsp4j = Lsp4jConversion.rangeToLsp4j(range);
        return new Hover(contents, rangeLsp4j);
    }

    private void provideHoverPackage(
            final MagikTypedFile magikFile,
            final AstNode hoveredNode,
            final StringBuilder builder) {
        final String packageName = hoveredNode.getTokenValue();
        final ITypeKeeper typeKeeper = magikFile.getTypeKeeper();
        final Package pakkage = typeKeeper.getPackage(packageName);
        if (pakkage == null) {
            return;
        }

        // Name.
        builder
            .append("## ")
            .append(pakkage.getName())
            .append(SECTION_END);

        // Doc.
        final String doc = pakkage.getDoc();
        if (doc != null) {
            final String docMd = doc.lines()
                .map(String::trim)
                .collect(Collectors.joining("\n\n"));
            builder
                .append(docMd)
                .append(SECTION_END);
        }

        // Uses.
        this.buildUsesDoc(pakkage, builder, 0);
    }

    private void buildUsesDoc(final Package pakkage, final StringBuilder builder, final int indent) {
        if (indent == 0) {
            builder
                .append(pakkage.getName())
                .append("\n\n");
        }

        final String indentStr = "&nbsp;&nbsp;".repeat(indent);
        final Comparator<Package> byName = Comparator.comparing(Package::getName);
        pakkage.getUsesPackages().stream()
            .sorted(byName)
            .forEach(uses -> {
                builder
                    .append(indentStr)
                    .append(" ↳ ")
                    .append(uses.getName())
                    .append("\n\n");

                this.buildUsesDoc(uses, builder, indent + 1);
            });

        if (indent == 0) {
            builder.append(SECTION_END);
        }
    }

    private void provideHoverCondition(
            final MagikTypedFile magikFile,
            final AstNode hoveredNode,
            final StringBuilder builder) {
        final String conditionName = hoveredNode.getTokenValue();
        final ITypeKeeper typeKeeper = magikFile.getTypeKeeper();
        final Condition condition = typeKeeper.getCondition(conditionName);
        if (condition == null) {
            return;
        }

        // Name.
        builder
            .append("## ")
            .append(condition.getName())
            .append(SECTION_END);

        // Doc.
        final String doc = condition.getDoc();
        if (doc != null) {
            final String docMd = doc.lines()
                .map(String::trim)
                .collect(Collectors.joining("\n\n"));
            builder
                .append(docMd)
                .append(SECTION_END);
        }

        // Taxonomy.
        builder.append("## Taxonomy: \n\n");
        String parentConditionName = condition.getParent();
        while (parentConditionName != null) {
            final Condition parentCondition = typeKeeper.getCondition(parentConditionName);
            if (parentCondition == null) {
                break;
            }

            final String parentName = parentCondition.getName();
            builder
                .append(parentName)
                .append("\n");

            parentConditionName = parentCondition.getParent();
        }
        builder.append(SECTION_END);

        // Data names.
        builder.append("## Data:\n");
        condition.getDataNameList().stream()
            .forEach(dataName ->
                builder
                    .append("* ")
                    .append(dataName)
                    .append("\n"));
    }

    private void provideHoverExpression(
            final MagikTypedFile magikFile,
            final AstNode expressionNode,
            final StringBuilder builder) {
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();
        final ExpressionResult result = reasonerState.getNodeTypeSilent(expressionNode);
        if (result != null) {
            LOGGER.debug("Providing hover for node: {}", expressionNode.getTokenValue());  // NOSONAR
            this.buildTypeDoc(magikFile, expressionNode, builder);
        }
    }

    /**
     * Provide hover for an atom.
     * @param magikFile Magik file.
     * @param atomNode Atom node hovered on.
     * @param builder Builder to add text to.
     */
    private void provideHoverAtom(final MagikTypedFile magikFile, final AstNode atomNode, final StringBuilder builder) {
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();
        final ExpressionResult result = reasonerState.getNodeTypeSilent(atomNode);
        if (result != null) {
            LOGGER.debug("Providing hover for node: {}", atomNode.getTokenValue());  // NOSONAR
            this.buildTypeDoc(magikFile, atomNode, builder);
        }
    }

    /**
     * Provide hover for a procedure invocation.
     * @param magikFile Magik file.
     * @param atomNode Hovered node.
     * @param builder Builder to add text to.
     */
    private void provideHoverProcedureInvocation(
            final MagikTypedFile magikFile,
            final AstNode hoveredNode,
            final StringBuilder builder) {
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();
        final AstNode providingNode = hoveredNode.getParent();
        if (providingNode != null) {
            final ExpressionResult result = reasonerState.getNodeType(providingNode);
            final AbstractType type = result.get(0, null);
            if (type != null) {
                LOGGER.debug("Providing hover for node: {}", providingNode.getTokenValue());  // NOSONAR
                this.buildProcDoc(magikFile, providingNode, builder);
            }
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
            LOGGER.debug(
                "Providing hover for node: {}, method: {}",
                previousSiblingNode.getTokenValue(), methodName);
            if (methodName != null) {
                final AbstractType type = this.getNodeType(magikFile, previousSiblingNode);
                final Collection<Method> methods = type.getMethods(methodName);
                methods.forEach(method -> this.buildMethodSignatureDoc(type, method, builder));
            }
        }
    }

    /**
     * Provide hover for a method definition.
     * @param magikFile Magik file.
     * @param hoveredNode Hovered node.
     * @param builder Builder to add text to.
     */
    private void provideHoverMethodDefinition(
            final MagikTypedFile magikFile,
            final AstNode hoveredNode,
            final StringBuilder builder) {
        final AstNode methodDefNode = hoveredNode.getFirstAncestor(MagikGrammar.METHOD_DEFINITION);
        final AstNode exemplarNameNode = methodDefNode.getFirstChild(MagikGrammar.EXEMPLAR_NAME);
        final AbstractType type = this.getNodeType(magikFile, exemplarNameNode);
        final MethodDefinitionNodeHelper methodDefHelper = new MethodDefinitionNodeHelper(methodDefNode);
        final String methodName = methodDefHelper.getMethodName();
        final Collection<Method> localMethods = type.getLocalMethods(methodName);
        localMethods.forEach(method -> this.buildMethodSignatureDoc(type, method, builder));
    }

    /**
     * Build hover text for type doc.
     * @param magikFile Magik file.
     * @param node {@link AstNode} to get info from.
     * @param builder {@link StringBuilder} to fill.
     */
    private void buildTypeDoc(
            final MagikTypedFile magikFile,
            final AstNode node,
            final StringBuilder builder) {
        // Get type.
        final AbstractType type = this.getNodeType(magikFile, node);
        this.buildTypeSignatureDoc(type, builder);
    }

    private void buildProcDoc(
            final MagikTypedFile magikFile,
            final AstNode node,
            final StringBuilder builder) {
        final AbstractType type = this.getNodeType(magikFile, node);
        final ProcedureInstance procInstance;
        if (type instanceof AliasType) {
            final AliasType aliasType = (AliasType) type;
            final AbstractType aliasedType = aliasType.getAliasedType();
            if (aliasedType instanceof ProcedureInstance) {
                procInstance = (ProcedureInstance) aliasedType;
            } else {
                procInstance = null;
            }
        } else if (type instanceof ProcedureInstance) {
            procInstance = (ProcedureInstance) type;
        } else {
            procInstance = null;
        }

        if (procInstance == null) {
            return;
        }

        final Method invokeMethod = procInstance.getInvokeMethod();
        this.buildMethodSignatureDoc(procInstance, invokeMethod, builder);
    }

    private void buildInheritanceDoc(final AbstractType type, final StringBuilder builder, final int indent) {
        final TypeString typeStr = type.getTypeString();
        if (indent == 0) {
            builder
                .append(this.formatTypeString(typeStr))
                .append("\n\n");
        }

        final String indentStr = "&nbsp;&nbsp;".repeat(indent);
        type.getParents().forEach(parentType -> {
            final TypeString parentTypeStr = parentType.getTypeString();
            builder
                .append(indentStr)
                .append(" ↳ ")
                .append(this.formatTypeString(parentTypeStr))
                .append("\n\n");

            this.buildInheritanceDoc(parentType, builder, indent + 1);
        });
    }

    private void buildMethodSignatureDoc(final AbstractType type, final Method method, final StringBuilder builder) {
        // Method signature.
        final TypeString typeStr = type.getTypeString();
        final AbstractType ownerType = method.getOwner();
        final TypeString ownerTypeStr = ownerType.getTypeString();
        final TypeString ownerTypeStrWithGenerics =
            TypeString.ofIdentifier(
                ownerTypeStr.getIdentifier(), ownerTypeStr.getPakkage(),
                typeStr.getGenerics().toArray(TypeString[]::new));

        final String joiner = method.getName().startsWith("[")
            ? ""
            : ".";
        builder
            .append("## ")
            .append(this.formatTypeString(ownerTypeStrWithGenerics))
            .append(joiner)
            .append(method.getNameWithParameters())
            .append("\n\n")
            .append(" → ");

        final String callResultString = method.getCallResult().getTypeNames(", ");
        builder.append(this.formatTypeString(callResultString));

        if (method.getModifiers().contains(Method.Modifier.ITER)) {
            builder
                .append("\n\n")
                .append(" ⟳ ");
            final String iterResultString = method.getLoopbodyResult().getTypeNames(", ");
            builder.append(this.formatTypeString(iterResultString));
        }

        builder.append(SECTION_END);

        // Method module.
        final String moduleName = Objects.requireNonNullElse(method.getModuleName(), "");
        builder
            .append("Module: ")
            .append(moduleName)
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
        final TypeString typeStr = type.getTypeString();
        builder
            .append("## ")
            .append(this.formatTypeString(typeStr))
            .append(SECTION_END);

        // Method module.
        final String moduleName = Objects.requireNonNullElse(type.getModuleName(), "");
        builder
            .append("Module: ")
            .append(moduleName)
            .append(SECTION_END);

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

        // Type slots.
        final Collection<Slot> slots = type.getSlots();
        if (!slots.isEmpty()) {
            builder
                .append("## Slots\n");
            slots.stream()
                .sorted(Comparator.comparing(Slot::getName))
                .forEach(slot -> {
                    final TypeString slotType = slot.getType();
                    builder
                        .append("* ")
                        .append(slot.getName())
                        .append(": ")
                        .append(this.formatTypeString(slotType))
                        .append("\n");
                });
            builder
                .append(SECTION_END);
        }

        // Type generic declarations.
        final Collection<GenericDefinition> genericDeclarations = type.getGenericDefinitions();
        if (!genericDeclarations.isEmpty()) {
            builder
                .append("## Generic definitions\n");
            genericDeclarations.stream()
                .forEach(generic -> {
                    final TypeString genericTypeStr = generic.getTypeString();
                    builder
                        .append("* ")
                        .append(this.formatTypeString(genericTypeStr))
                        .append("\n");
                });
            builder
                .append(SECTION_END);
        }

        // Inheritance.
        if (!type.getParents().isEmpty()) {
            builder
                .append("## Inheritance\n");
            if (type instanceof CombinedType) {
                final CombinedType combinedType = (CombinedType) type;
                combinedType.getTypes().stream()
                    .sorted(Comparator.comparing(AbstractType::getTypeString))
                    .forEach(cType -> this.buildInheritanceDoc(cType, builder, 0));
            } else {
                this.buildInheritanceDoc(type, builder, 0);
            }
            builder
                .append(SECTION_END);
        }
    }

    private AbstractType getNodeType(final MagikTypedFile magikFile, final AstNode node) {
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();
        final ExpressionResult result = reasonerState.getNodeType(node);
        final AbstractType type = result.get(0, UndefinedType.INSTANCE);
        if (type == SelfType.INSTANCE) {
            final AstNode defNode = node.getFirstAncestor(
                MagikGrammar.PROCEDURE_DEFINITION,
                MagikGrammar.METHOD_DEFINITION);
            if (defNode.is(MagikGrammar.PROCEDURE_DEFINITION)) {
                final ExpressionResult defResult = reasonerState.getNodeType(defNode);
                return defResult.get(0, UndefinedType.INSTANCE);
            } else if (defNode.is(MagikGrammar.METHOD_DEFINITION)) {
                final AstNode exemplaNameNode = defNode.getFirstChild(MagikGrammar.EXEMPLAR_NAME);
                final ExpressionResult defResult = reasonerState.getNodeType(exemplaNameNode);
                return defResult.get(0, UndefinedType.INSTANCE);
            } else {
                throw new IllegalStateException();
            }
        }

        return type;
    }

    private String formatTypeString(final TypeString typeStr) {
        return this.formatTypeString(typeStr.getFullString());
    }

    private String formatTypeString(final String typeStr) {
        return typeStr.replace("<", "[").replace(">", "]");
    }

}
