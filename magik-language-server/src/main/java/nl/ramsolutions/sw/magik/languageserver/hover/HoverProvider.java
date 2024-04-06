package nl.ramsolutions.sw.magik.languageserver.hover;

import com.sonar.sslr.api.AstNode;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.analysis.definitions.ConditionDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.PackageDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ProcedureDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.SlotDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.TypeStringDefinition;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodInvocationNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeStringResolver;
import nl.ramsolutions.sw.magik.analysis.typing.reasoner.LocalTypeReasonerState;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.languageserver.Lsp4jConversion;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.MarkupKind;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.ServerCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Hover provider. */
public class HoverProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(HoverProvider.class);

  private static final String SECTION_END = "\n\n---\n\n";

  /**
   * Set server capabilities.
   *
   * @param capabilities Server capabilities.
   */
  public void setCapabilities(final ServerCapabilities capabilities) {
    capabilities.setHoverProvider(true);
  }

  /**
   * Provide a hover at the given position.
   *
   * @param magikFile Magik file.
   * @param position Position in file.
   * @return Hover at position.
   */
  @SuppressWarnings("java:S3776")
  public Hover provideHover(final MagikTypedFile magikFile, final Position position) {
    // Parse and reason magik.
    final AstNode node = magikFile.getTopNode();
    final AstNode hoveredTokenNode =
        AstQuery.nodeAt(node, Lsp4jConversion.positionFromLsp4j(position));
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

    final String content = builder.isEmpty() ? "Undefined" : builder.toString();
    final MarkupContent contents = new MarkupContent(MarkupKind.MARKDOWN, content);
    final Range range = new Range(hoveredTokenNode);
    final org.eclipse.lsp4j.Range rangeLsp4j = Lsp4jConversion.rangeToLsp4j(range);
    return new Hover(contents, rangeLsp4j);
  }

  private void provideHoverPackage(
      final MagikTypedFile magikFile, final AstNode hoveredNode, final StringBuilder builder) {
    final String packageName = hoveredNode.getTokenValue();
    final IDefinitionKeeper definitionKeeper = magikFile.getDefinitionKeeper();
    definitionKeeper.getPackageDefinitions(packageName).stream()
        .forEach(
            pakkageDef -> {
              // Name.
              builder.append("## ").append(pakkageDef.getName()).append(SECTION_END);

              // Doc.
              final String doc = pakkageDef.getDoc();
              if (doc != null) {
                final String docMd =
                    doc.lines().map(String::trim).collect(Collectors.joining("\n\n"));
                builder.append(docMd).append(SECTION_END);
              }

              // Uses.
              this.addPackageHierarchy(magikFile, pakkageDef, builder, 0);
            });
  }

  private void addPackageHierarchy(
      final MagikTypedFile magikFile,
      final PackageDefinition pakkageDef,
      final StringBuilder builder,
      final int indent) {
    if (indent == 0) {
      builder.append(pakkageDef.getName()).append("\n\n");
    }

    final IDefinitionKeeper definitionKeeper = magikFile.getDefinitionKeeper();
    final String indentStr = "&nbsp;&nbsp;".repeat(indent);
    pakkageDef.getUses().stream()
        .sorted()
        .forEach(
            use -> {
              builder.append(indentStr).append(" ↳ ").append(use).append("\n\n");

              definitionKeeper
                  .getPackageDefinitions(use)
                  .forEach(
                      usePakkageDef ->
                          this.addPackageHierarchy(magikFile, usePakkageDef, builder, indent + 1));
            });

    if (indent == 0) {
      builder.append(SECTION_END);
    }
  }

  private void provideHoverCondition(
      final MagikTypedFile magikFile, final AstNode hoveredNode, final StringBuilder builder) {
    final String conditionName = hoveredNode.getTokenValue();
    final IDefinitionKeeper definitionKeeper = magikFile.getDefinitionKeeper();
    definitionKeeper
        .getConditionDefinitions(conditionName)
        .forEach(
            conditionDef -> {
              // Name.
              builder.append("## ").append(conditionDef.getName()).append(SECTION_END);

              // Doc.
              final String doc = conditionDef.getDoc();
              if (doc != null) {
                final String docMd =
                    doc.lines().map(String::trim).collect(Collectors.joining("\n\n"));
                builder.append(docMd).append(SECTION_END);
              }

              // Taxonomy.
              builder.append("## Taxonomy: \n\n");
              this.addConditionTaxonomy(magikFile, conditionDef, builder, 0);
              builder.append(SECTION_END);

              // Data names.
              builder.append("## Data:\n");
              conditionDef
                  .getDataNames()
                  .forEach(dataName -> builder.append("* ").append(dataName).append("\n"));
            });
  }

  private void addConditionTaxonomy(
      final MagikTypedFile magikFile,
      final ConditionDefinition conditionDefinition,
      final StringBuilder builder,
      final int indent) {
    if (indent == 0) {
      builder.append(conditionDefinition.getName()).append("\n\n");
    }

    final IDefinitionKeeper definitionKeeper = magikFile.getDefinitionKeeper();
    final String indentStr = "&nbsp;&nbsp;".repeat(indent);
    final String parentConditionName = conditionDefinition.getParent();
    if (parentConditionName != null) {
      builder.append(indentStr).append(" ↳ ").append(parentConditionName).append("\n\n");

      definitionKeeper
          .getConditionDefinitions(parentConditionName)
          .forEach(
              parentConditionDef ->
                  this.addConditionTaxonomy(magikFile, parentConditionDef, builder, indent + 1));
    }

    if (indent == 0) {
      builder.append(SECTION_END);
    }
  }

  private void provideHoverExpression(
      final MagikTypedFile magikFile, final AstNode expressionNode, final StringBuilder builder) {
    final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();
    final ExpressionResultString result = reasonerState.getNodeTypeSilent(expressionNode);
    if (result != null) {
      LOGGER.debug("Providing hover for node: {}", expressionNode.getTokenValue()); // NOSONAR
      this.buildTypeDoc(magikFile, expressionNode, builder);
    }
  }

  /**
   * Provide hover for an atom.
   *
   * @param magikFile Magik file.
   * @param atomNode Atom node hovered on.
   * @param builder Builder to add text to.
   */
  private void provideHoverAtom(
      final MagikTypedFile magikFile, final AstNode atomNode, final StringBuilder builder) {
    final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();
    final ExpressionResultString result = reasonerState.getNodeTypeSilent(atomNode);
    if (result != null) {
      LOGGER.debug("Providing hover for node: {}", atomNode.getTokenValue()); // NOSONAR
      this.buildTypeDoc(magikFile, atomNode, builder);
    }
  }

  /**
   * Provide hover for a procedure invocation.
   *
   * @param magikFile Magik file.
   * @param atomNode Hovered node.
   * @param builder Builder to add text to.
   */
  private void provideHoverProcedureInvocation(
      final MagikTypedFile magikFile, final AstNode hoveredNode, final StringBuilder builder) {
    final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();
    final AstNode providingNode = hoveredNode.getParent();
    if (providingNode != null) {
      final ExpressionResultString result = reasonerState.getNodeType(providingNode);
      final TypeString typeStr = result.get(0, null);
      if (typeStr != null) {
        LOGGER.debug("Providing hover for node: {}", providingNode.getTokenValue()); // NOSONAR
        this.buildProcDoc(magikFile, providingNode, builder);
      }
    }
  }

  /**
   * Provide hover for a method invocation.
   *
   * @param magikFile Magik file.
   * @param hoveredNode Hovered node.
   * @param builder Builder to add text to.
   */
  private void provideHoverMethodInvocation(
      final MagikTypedFile magikFile, final AstNode hoveredNode, final StringBuilder builder) {
    final AstNode providingNode = hoveredNode.getParent();
    final AstNode previousSiblingNode = providingNode.getPreviousSibling();
    if (previousSiblingNode != null) {
      final MethodInvocationNodeHelper helper = new MethodInvocationNodeHelper(providingNode);
      final String methodName = helper.getMethodName();
      LOGGER.debug(
          "Providing hover for node: {}, method: {}",
          previousSiblingNode.getTokenValue(),
          methodName);
      if (methodName != null) {
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();
        final ExpressionResultString result = reasonerState.getNodeType(previousSiblingNode);
        final TypeString typeStr = result.get(0, TypeString.UNDEFINED);
        final TypeStringResolver resolver = magikFile.getTypeStringResolver();
        resolver
            .getMethodDefinitions(typeStr)
            .forEach(methodDef -> this.buildMethodSignatureDoc(methodDef, builder));
      }
    }
  }

  /**
   * Provide hover for a method definition.
   *
   * @param magikFile Magik file.
   * @param hoveredNode Hovered node.
   * @param builder Builder to add text to.
   */
  private void provideHoverMethodDefinition(
      final MagikTypedFile magikFile, final AstNode hoveredNode, final StringBuilder builder) {
    final AstNode methodDefNode = hoveredNode.getFirstAncestor(MagikGrammar.METHOD_DEFINITION);
    final AstNode exemplarNameNode = methodDefNode.getFirstChild(MagikGrammar.EXEMPLAR_NAME);
    final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();
    final ExpressionResultString result = reasonerState.getNodeType(exemplarNameNode);
    final TypeString typeStr = result.get(0, TypeString.UNDEFINED);

    final MethodDefinitionNodeHelper methodDefHelper =
        new MethodDefinitionNodeHelper(methodDefNode);
    final String methodName = methodDefHelper.getMethodName();
    final TypeStringResolver resolver = magikFile.getTypeStringResolver();
    resolver.getMethodDefinitions(typeStr, methodName).stream()
        .forEach(methodDef -> this.buildMethodSignatureDoc(methodDef, builder));
  }

  /**
   * Build hover text for type doc.
   *
   * @param magikFile Magik file.
   * @param node {@link AstNode} to get info from.
   * @param builder {@link StringBuilder} to fill.
   */
  private void buildTypeDoc(
      final MagikTypedFile magikFile, final AstNode node, final StringBuilder builder) {
    final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();
    final ExpressionResultString result = reasonerState.getNodeType(node);
    final TypeString typeStr = result.get(0, TypeString.UNDEFINED);
    final IDefinitionKeeper definitionKeeper = magikFile.getDefinitionKeeper();
    definitionKeeper
        .getExemplarDefinitions(typeStr)
        .forEach(exemplarDef -> this.buildTypeSignatureDoc(magikFile, exemplarDef, builder));
  }

  private void buildProcDoc(
      final MagikTypedFile magikFile, final AstNode node, final StringBuilder builder) {
    final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();
    final ExpressionResultString result = reasonerState.getNodeType(node);
    final TypeString typeStr = result.get(0, TypeString.UNDEFINED);
    final TypeStringDefinition typeStringDefinition =
        reasonerState.getTypeStringDefinition(typeStr);
    // TODO: Removed some resolving of aliases etc.
    if (typeStringDefinition instanceof ProcedureDefinition procedureDefinition) {
      this.buildProcSignatureDoc(procedureDefinition, builder);
    }
  }

  private void addSuperDoc(
      final MagikTypedFile magikFile,
      final ExemplarDefinition exemplarDef,
      final StringBuilder builder,
      final int indent) {
    final TypeString typeStr = exemplarDef.getTypeString();
    if (indent == 0) {
      builder.append(this.formatTypeString(typeStr)).append("\n\n");
    }

    final IDefinitionKeeper definitionKeeper = magikFile.getDefinitionKeeper();
    final String indentStr = "&nbsp;&nbsp;".repeat(indent);
    exemplarDef
        .getParents()
        .forEach(
            parentTypeStr -> {
              builder
                  .append(indentStr)
                  .append(" ↳ ")
                  .append(this.formatTypeString(parentTypeStr))
                  .append("\n\n");

              definitionKeeper
                  .getExemplarDefinitions(parentTypeStr)
                  .forEach(
                      parentExemplarDef ->
                          this.addSuperDoc(magikFile, parentExemplarDef, builder, indent + 1));
            });
  }

  private void buildMethodSignatureDoc(
      final MethodDefinition methodDef, final StringBuilder builder) {
    // Method signature.
    final TypeString typeStr = methodDef.getTypeName();

    // TODO: Removed something with generics.

    final String joiner = methodDef.getMethodName().startsWith("[") ? "" : ".";
    builder
        .append("## ")
        .append(this.formatTypeString(typeStr))
        .append(joiner)
        .append(methodDef.getNameWithParameters())
        .append("\n\n")
        .append(" → ");

    final String callResultString = methodDef.getReturnTypes().getTypeNames(", ");
    builder.append(this.formatTypeString(callResultString));

    if (methodDef.getModifiers().contains(MethodDefinition.Modifier.ITER)) {
      builder.append("\n\n").append(" ⟳ ");
      final String iterResultString = methodDef.getLoopTypes().getTypeNames(", ");
      builder.append(this.formatTypeString(iterResultString));
    }

    builder.append(SECTION_END);

    // Method module.
    final String moduleName = Objects.requireNonNullElse(methodDef.getModuleName(), "");
    builder.append("Module: ").append(moduleName).append(SECTION_END);

    // Method topics.
    final String topics = methodDef.getTopics().stream().collect(Collectors.joining(", "));
    builder.append("Topics: ").append(topics).append(SECTION_END);

    // Method doc.
    final String methodDoc = methodDef.getDoc();
    if (methodDoc != null) {
      final String methodDocMd =
          methodDoc.lines().map(String::trim).collect(Collectors.joining("\n\n"));
      builder.append(methodDocMd).append(SECTION_END);
    }
  }

  private void buildProcSignatureDoc(
      final ProcedureDefinition procDef, final StringBuilder builder) {
    final TypeString typeStr = procDef.getTypeString();

    // TODO: Removed something with generics.

    final String joiner = procDef.getName().startsWith("[") ? "" : ".";
    builder
        .append("## ")
        .append(this.formatTypeString(typeStr))
        .append(joiner)
        .append(procDef.getNameWithParameters())
        .append("\n\n")
        .append(" → ");

    final String callResultString = procDef.getReturnTypes().getTypeNames(", ");
    builder.append(this.formatTypeString(callResultString));

    if (procDef.getModifiers().contains(ProcedureDefinition.Modifier.ITER)) {
      builder.append("\n\n").append(" ⟳ ");
      final String iterResultString = procDef.getLoopTypes().getTypeNames(", ");
      builder.append(this.formatTypeString(iterResultString));
    }

    builder.append(SECTION_END);

    // Procedure module.
    final String moduleName = Objects.requireNonNullElse(procDef.getModuleName(), "");
    builder.append("Module: ").append(moduleName).append(SECTION_END);

    // TODO: Procedure topics.
    // final String topics = procDef.getTopics().stream().collect(Collectors.joining(", "));
    // builder.append("Topics: ").append(topics).append(SECTION_END);

    // Procedure doc.
    final String methodDoc = procDef.getDoc();
    if (methodDoc != null) {
      final String methodDocMd =
          methodDoc.lines().map(String::trim).collect(Collectors.joining("\n\n"));
      builder.append(methodDocMd).append(SECTION_END);
    }
  }

  private void buildTypeSignatureDoc(
      final MagikTypedFile magikFile,
      final ExemplarDefinition exemplarDef,
      final StringBuilder builder) {
    final TypeString typeStr = exemplarDef.getTypeString();
    builder.append("## ").append(this.formatTypeString(typeStr)).append(SECTION_END);

    final String moduleName = Objects.requireNonNullElse(exemplarDef.getModuleName(), "");
    builder.append("Module: ").append(moduleName).append(SECTION_END);

    final String topics = exemplarDef.getTopics().stream().collect(Collectors.joining(", "));
    builder.append("Topics: ").append(topics).append(SECTION_END);

    final String typeDoc = exemplarDef.getDoc();
    if (typeDoc != null) {
      final String typeDocMd =
          typeDoc.lines().map(String::trim).collect(Collectors.joining("\n\n"));
      builder.append(typeDocMd).append(SECTION_END);
    }

    final Collection<SlotDefinition> slots = exemplarDef.getSlots();
    if (!slots.isEmpty()) {
      builder.append("## Slots\n");
      slots.stream()
          .sorted(Comparator.comparing(SlotDefinition::getName))
          .forEach(
              slot -> {
                final TypeString slotType = slot.getTypeName();
                builder
                    .append("* ")
                    .append(slot.getName())
                    .append(": ")
                    .append(this.formatTypeString(slotType))
                    .append("\n");
              });
      builder.append(SECTION_END);
    }

    final List<TypeString> generics = exemplarDef.getTypeString().getGenerics();
    if (!generics.isEmpty()) {
      builder.append("## Generic definitions\n");
      generics.stream()
          .forEach(
              genericTypeStr ->
                  builder.append("* ").append(this.formatTypeString(genericTypeStr)).append("\n"));
      builder.append(SECTION_END);
    }

    if (!exemplarDef.getParents().isEmpty()) {
      builder.append("## Supers\n");
      this.addSuperDoc(magikFile, exemplarDef, builder, 0);
      builder.append(SECTION_END);
    }
  }

  private String formatTypeString(final TypeString typeStr) {
    return this.formatTypeString(typeStr.getFullString());
  }

  private String formatTypeString(final String typeStr) {
    return typeStr.replace("<", "[").replace(">", "]");
  }
}
