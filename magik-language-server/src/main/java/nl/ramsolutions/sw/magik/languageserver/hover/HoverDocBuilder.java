package nl.ramsolutions.sw.magik.languageserver.hover;

import com.sonar.sslr.api.AstNode;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.ITypeStringDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ProcedureDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.SlotDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.SelfHelper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeStringResolver;
import nl.ramsolutions.sw.magik.analysis.typing.reasoner.LocalTypeReasonerState;

/** Builds the documentation sections shared by the {@link HoverModule}s. */
final class HoverDocBuilder {

  private HoverDocBuilder() {}

  /**
   * Build hover text for the type of a node.
   *
   * @param magikFile Magik file.
   * @param node {@link AstNode} to get info from.
   * @param builder {@link StringBuilder} to fill.
   */
  static void buildTypeDoc(
      final MagikTypedFile magikFile, final AstNode node, final StringBuilder builder) {
    final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();
    final ExpressionResultString result = reasonerState.getNodeType(node);
    final TypeString resultTypeStr = result.get(0, TypeString.UNDEFINED);
    final TypeString typeStr = SelfHelper.substituteSelf(resultTypeStr, node);

    // A combined type has no identifier of its own to resolve, so document each of its parts.
    final TypeStringResolver resolver = magikFile.getTypeStringResolver();
    typeStr.getCombinedTypes().stream()
        .flatMap(combinedTypeStr -> resolver.resolve(combinedTypeStr).stream())
        .filter(ExemplarDefinition.class::isInstance)
        .map(ExemplarDefinition.class::cast)
        .forEach(
            exemplarDef -> HoverDocBuilder.buildTypeSignatureDoc(magikFile, exemplarDef, builder));
  }

  /**
   * Build hover text for the procedure a node resolves to.
   *
   * @param magikFile Magik file.
   * @param node {@link AstNode} to get info from.
   * @param builder {@link StringBuilder} to fill.
   */
  static void buildProcDoc(
      final MagikTypedFile magikFile, final AstNode node, final StringBuilder builder) {
    final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();
    final ExpressionResultString result = reasonerState.getNodeType(node);
    final TypeString typeStr = result.get(0, TypeString.UNDEFINED);
    final TypeStringResolver resolver = magikFile.getTypeStringResolver();
    final Collection<ITypeStringDefinition> typeDefs = resolver.resolve(typeStr);
    final ITypeStringDefinition typeDef = typeDefs.isEmpty() ? null : typeDefs.iterator().next();
    if (typeDef instanceof ProcedureDefinition procedureDefinition) {
      HoverDocBuilder.buildProcSignatureDoc(procedureDefinition, builder);
    }
  }

  /**
   * Build hover text for a method definition.
   *
   * @param methodDef Method definition.
   * @param builder {@link StringBuilder} to fill.
   */
  static void buildMethodSignatureDoc(
      final MethodDefinition methodDef, final StringBuilder builder) {
    builder.append("## ").append(methodDef.getNameWithParameters()).append("\n\n").append(" → ");

    final String callResultString = methodDef.getReturnTypes().getTypeNames(", ");
    builder.append(HoverUtils.formatTypeString(callResultString));

    if (methodDef.getModifiers().contains(MethodDefinition.Modifier.ITER)) {
      builder.append("\n\n").append(" ⟳ ");
      final String iterResultString = methodDef.getLoopTypes().getTypeNames(", ");
      builder.append(HoverUtils.formatTypeString(iterResultString));
    }

    builder.append(HoverUtils.SECTION_END);

    // Method module.
    final String moduleName = Objects.requireNonNullElse(methodDef.getModuleName(), "");
    builder.append("Module: ").append(moduleName).append(HoverUtils.SECTION_END);

    // Method topics.
    final String topics =
        methodDef.getPragma() != null
            ? methodDef.getPragma().getTopics().stream().collect(Collectors.joining(", "))
            : "";
    builder.append("Topics: ").append(topics).append(HoverUtils.SECTION_END);

    // Method doc.
    final String methodDoc = methodDef.getDoc();
    if (methodDoc != null) {
      builder.append(HoverUtils.docToMarkdown(methodDoc)).append(HoverUtils.SECTION_END);
    }
  }

  private static void buildProcSignatureDoc(
      final ProcedureDefinition procDef, final StringBuilder builder) {
    final TypeString typeStr = procDef.getTypeString();

    final String aliasName = typeStr.isAnonymous() ? "proc" : HoverUtils.formatTypeString(typeStr);

    builder.append("## ").append(aliasName).append("()").append("\n\n").append(" → ");

    final String callResultString = procDef.getReturnTypes().getTypeNames(", ");
    builder.append(HoverUtils.formatTypeString(callResultString));

    if (procDef.getModifiers().contains(ProcedureDefinition.Modifier.ITER)) {
      builder.append("\n\n").append(" ⟳ ");
      final String iterResultString = procDef.getLoopTypes().getTypeNames(", ");
      builder.append(HoverUtils.formatTypeString(iterResultString));
    }

    builder.append(HoverUtils.SECTION_END);

    // Procedure module.
    final String moduleName = Objects.requireNonNullElse(procDef.getModuleName(), "");
    builder.append("Module: ").append(moduleName).append(HoverUtils.SECTION_END);

    // Procedure topics.
    final String topics =
        procDef.getPragma() != null
            ? procDef.getPragma().getTopics().stream().collect(Collectors.joining(", "))
            : "";
    builder.append("Topics: ").append(topics).append(HoverUtils.SECTION_END);

    // Procedure doc.
    final String methodDoc = procDef.getDoc();
    if (methodDoc != null) {
      builder.append(HoverUtils.docToMarkdown(methodDoc)).append(HoverUtils.SECTION_END);
    }
  }

  private static void buildTypeSignatureDoc(
      final MagikTypedFile magikFile,
      final ExemplarDefinition exemplarDef,
      final StringBuilder builder) {
    final TypeString typeStr = exemplarDef.getTypeString();
    builder
        .append("## ")
        .append(HoverUtils.formatTypeString(typeStr))
        .append(HoverUtils.SECTION_END);

    final String moduleName = Objects.requireNonNullElse(exemplarDef.getModuleName(), "");
    builder.append("Module: ").append(moduleName).append(HoverUtils.SECTION_END);

    final String topics =
        exemplarDef.getPragma() != null
            ? exemplarDef.getPragma().getTopics().stream().collect(Collectors.joining(", "))
            : "";
    builder.append("Topics: ").append(topics).append(HoverUtils.SECTION_END);

    final String typeDoc = exemplarDef.getDoc();
    if (typeDoc != null) {
      builder.append(HoverUtils.docToMarkdown(typeDoc)).append(HoverUtils.SECTION_END);
    }

    final Collection<SlotDefinition> slots =
        magikFile.getTypeStringResolver().getSlotDefinitions(typeStr);
    if (!slots.isEmpty()) {
      builder.append("## Slots\n");
      slots.stream()
          .sorted(Comparator.comparing(SlotDefinition::getName))
          .forEach(
              slotDef -> {
                final TypeString slotTypeStr = slotDef.getTypeName();
                builder
                    .append("* ")
                    .append(slotDef.getName())
                    .append(": ")
                    .append(HoverUtils.formatTypeString(slotTypeStr));
                if (!slotDef.getOwnerTypeName().equals(typeStr)) {
                  builder
                      .append(" _(from ")
                      .append(HoverUtils.formatTypeString(slotDef.getOwnerTypeName()))
                      .append(")_");
                }
                builder.append("\n");
              });
      builder.append(HoverUtils.SECTION_END);
    }

    final List<TypeString> generics = exemplarDef.getTypeString().getGenerics();
    if (!generics.isEmpty()) {
      builder.append("## Generic definitions\n");
      generics.forEach(
          genericTypeStr ->
              builder
                  .append("* ")
                  .append(HoverUtils.formatTypeString(genericTypeStr))
                  .append("\n"));
      builder.append(HoverUtils.SECTION_END);
    }

    final TypeStringResolver resolver = magikFile.getTypeStringResolver();
    if (!resolver.getParents(typeStr).isEmpty()) {
      builder.append("## Supers\n");
      HoverDocBuilder.addSuperDoc(magikFile, exemplarDef, builder, 0);
      builder.append(HoverUtils.SECTION_END);
    }
  }

  private static void addSuperDoc(
      final MagikTypedFile magikFile,
      final ExemplarDefinition exemplarDef,
      final StringBuilder builder,
      final int indent) {
    final TypeString typeStr = exemplarDef.getTypeString();
    if (indent == 0) {
      builder.append(HoverUtils.formatTypeString(typeStr)).append("\n\n");
    }

    final IDefinitionKeeper definitionKeeper = magikFile.getDefinitionKeeper();
    final String indentStr = HoverUtils.NBSP_NBSP.repeat(indent);
    final TypeStringResolver resolver = magikFile.getTypeStringResolver();
    resolver
        .getParents(typeStr)
        .forEach(
            parentTypeStr -> {
              builder
                  .append(indentStr)
                  .append(" ↳ ")
                  .append(HoverUtils.formatTypeString(parentTypeStr))
                  .append("\n\n");

              definitionKeeper
                  .getExemplarDefinitions(parentTypeStr)
                  .forEach(
                      parentExemplarDef ->
                          HoverDocBuilder.addSuperDoc(
                              magikFile, parentExemplarDef, builder, indent + 1));
            });
  }
}
