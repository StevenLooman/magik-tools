package nl.ramsolutions.sw.checks.magiktyped;

import com.sonar.sslr.api.AstNode;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import nl.ramsolutions.sw.checks.MagikTypedCheck;
import nl.ramsolutions.sw.magik.analysis.definitions.SlotDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.parsers.DefSlottedExemplarParser;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.ProcedureDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeStringResolver;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.parser.TypeDocParser;
import org.sonar.check.Rule;

/** Test if referenced type is known. */
@Rule(key = TypeDocTypeExistsTypedCheck.CHECK_KEY)
public class TypeDocTypeExistsTypedCheck extends MagikTypedCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "TypeDocTypeExists";

  private static final String MESSAGE = "Unknown type: %s";
  private static final String MESSAGE_UNKNOWN_PARAMETER = "Unknown parameter reference: %s";
  private static final String MESSAGE_UNKNOWN_SLOT = "Unknown slot reference: %s";

  @Override
  protected void walkPostMethodDefinition(final AstNode node) {
    this.checkMethodProcedureDefinition(node);
  }

  @Override
  protected void walkPostProcedureDefinition(final AstNode node) {
    this.checkMethodProcedureDefinition(node);
  }

  private void checkMethodProcedureDefinition(final AstNode node) {
    final TypeDocParser typeDocParser = new TypeDocParser(node);
    this.checkDefinitionParameters(typeDocParser, node);
    this.checkDefinitionLoops(typeDocParser, node);
    this.checkDefinitionReturns(typeDocParser, node);
  }

  private void checkDefinitionParameters(
      final TypeDocParser typeDocParser, final AstNode definitionNode) {
    typeDocParser.getParameterTypeNodes().entrySet().stream()
        .forEach(entry -> this.checkTypeDocEntry(entry, definitionNode));
  }

  private void checkDefinitionLoops(
      final TypeDocParser typeDocParser, final AstNode definitionNode) {
    typeDocParser.getLoopTypeNodes().entrySet().stream()
        .forEach(entry -> this.checkTypeDocEntry(entry, definitionNode));
  }

  private void checkDefinitionReturns(
      final TypeDocParser typeDocParser, final AstNode definitionNode) {
    typeDocParser.getReturnTypeNodes().entrySet().stream()
        .forEach(entry -> this.checkTypeDocEntry(entry, definitionNode));
  }

  @Override
  protected void walkPostProcedureInvocation(final AstNode node) {
    if (!DefSlottedExemplarParser.isDefSlottedExemplar(node)) {
      return;
    }

    final AstNode statementNode = node.getFirstAncestor(MagikGrammar.STATEMENT);
    final TypeDocParser typeDocParser = new TypeDocParser(statementNode);
    final Map<AstNode, TypeString> slotTypeNodes = typeDocParser.getSlotTypeNodes();

    slotTypeNodes.entrySet().stream().forEach(entry -> this.checkTypeDocEntry(entry, null));
  }

  private void checkTypeDocEntry(
      final Map.Entry<AstNode, TypeString> entry, final AstNode definitionNode) {
    final AstNode typeNode = entry.getKey();
    final TypeString rawTypeStr = entry.getValue();
    final TypeString typeStr = rawTypeStr.isVariadic() ? rawTypeStr.getVariadicInner() : rawTypeStr;

    if (typeStr.isSelf() || typeStr.isPrivate()) {
      return;
    }

    if (typeStr.isParameterReference()) {
      this.checkParameterRef(typeNode, typeStr, definitionNode);
      return;
    }

    if (typeStr.isSlotReference()) {
      this.checkSlotRef(typeNode, typeStr, definitionNode);
      return;
    }

    final TypeString combinedTypeStr = TypeString.combine(typeStr);
    Objects.requireNonNull(combinedTypeStr);
    final TypeStringResolver resolver = this.getTypeStringResolver();
    for (final TypeString typeString : combinedTypeStr.getCombinedTypes()) {
      if (resolver.getExemplarDefinition(typeString) == null) {
        this.addIssue(typeNode, MESSAGE.formatted(typeString.getFullString()));
        return;
      }
    }
  }

  private void checkParameterRef(
      final AstNode typeNode, final TypeString typeStr, final AstNode definitionNode) {
    if (definitionNode != null && !this.parameterExists(typeStr.getIdentifier(), definitionNode)) {
      this.addIssue(typeNode, MESSAGE_UNKNOWN_PARAMETER.formatted(typeStr.getIdentifier()));
    }
  }

  private void checkSlotRef(
      final AstNode typeNode, final TypeString typeStr, final AstNode definitionNode) {
    if (definitionNode != null
        && definitionNode.is(MagikGrammar.METHOD_DEFINITION)
        && !this.slotExists(typeStr.getIdentifier(), definitionNode)) {
      this.addIssue(typeNode, MESSAGE_UNKNOWN_SLOT.formatted(typeStr.getIdentifier()));
    }
  }

  private boolean parameterExists(final String name, final AstNode definitionNode) {
    if (definitionNode.is(MagikGrammar.METHOD_DEFINITION)) {
      return new MethodDefinitionNodeHelper(definitionNode).getParameterNodes().containsKey(name);
    }
    return new ProcedureDefinitionNodeHelper(definitionNode).getParameterNodes().containsKey(name);
  }

  private boolean slotExists(final String name, final AstNode definitionNode) {
    final MethodDefinitionNodeHelper helper = new MethodDefinitionNodeHelper(definitionNode);
    final TypeString receiverType = helper.getTypeString();
    final TypeStringResolver resolver = this.getTypeStringResolver();
    final Collection<SlotDefinition> slots = resolver.getSlotDefinitions(receiverType);
    return slots.stream().anyMatch(slot -> slot.getName().equals(name));
  }
}
