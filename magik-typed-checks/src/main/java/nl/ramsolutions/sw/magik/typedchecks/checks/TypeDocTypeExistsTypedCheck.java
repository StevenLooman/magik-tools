package nl.ramsolutions.sw.magik.typedchecks.checks;

import com.sonar.sslr.api.AstNode;
import java.util.Map;
import nl.ramsolutions.sw.magik.analysis.definitions.parsers.DefSlottedExemplarParser;
import nl.ramsolutions.sw.magik.analysis.typing.TypeStringResolver;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.parser.TypeDocParser;
import nl.ramsolutions.sw.magik.typedchecks.MagikTypedCheck;
import org.sonar.check.Rule;

/** Test if referenced type is known. */
@Rule(key = TypeDocTypeExistsTypedCheck.CHECK_KEY)
public class TypeDocTypeExistsTypedCheck extends MagikTypedCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "TypeDocTypeExists";

  private static final String MESSAGE = "Unknown type: %s";

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
    this.checkDefinitionParameters(typeDocParser);
    this.checkDefinitionLoops(typeDocParser);
    this.checkDefinitionReturns(typeDocParser);
  }

  private void checkDefinitionParameters(final TypeDocParser typeDocParser) {
    // Test @param types.
    typeDocParser.getParameterTypeNodes().entrySet().stream()
        .filter(this::unknownExemplarDefinitionEntry)
        .forEach(
            entry -> {
              final AstNode typeNode = entry.getKey();
              final TypeString typeString = entry.getValue();
              final String message = String.format(MESSAGE, typeString.getFullString());
              this.addIssue(typeNode, message);
            });
  }

  private void checkDefinitionLoops(final TypeDocParser typeDocParser) {
    // Test @loop types.
    typeDocParser.getLoopTypeNodes().entrySet().stream()
        .filter(this::unknownExemplarDefinitionEntry)
        .forEach(
            entry -> {
              final AstNode typeNode = entry.getKey();
              final TypeString typeString = entry.getValue();
              final String message = String.format(MESSAGE, typeString.getFullString());
              this.addIssue(typeNode, message);
            });
  }

  private void checkDefinitionReturns(final TypeDocParser typeDocParser) {
    // Test @return types.
    typeDocParser.getReturnTypeNodes().entrySet().stream()
        .filter(this::unknownExemplarDefinitionEntry)
        .forEach(
            entry -> {
              final AstNode typeNode = entry.getKey();
              final TypeString typeString = entry.getValue();
              final String message = String.format(MESSAGE, typeString.getFullString());
              this.addIssue(typeNode, message);
            });
  }

  @Override
  protected void walkPostProcedureInvocation(final AstNode node) {
    if (!DefSlottedExemplarParser.isDefSlottedExemplar(node)) {
      return;
    }

    // Get slot defintions.
    final AstNode statementNode = node.getFirstAncestor(MagikGrammar.STATEMENT);
    final TypeDocParser typeDocParser = new TypeDocParser(statementNode);
    final Map<AstNode, TypeString> slotTypeNodes = typeDocParser.getSlotTypeNodes();

    // Test slot types.
    slotTypeNodes.entrySet().stream()
        .filter(this::unknownExemplarDefinitionEntry)
        .forEach(
            entry -> {
              final AstNode typeNode = entry.getKey();
              final TypeString typeString = entry.getValue();
              final String message = String.format(MESSAGE, typeString.getFullString());
              this.addIssue(typeNode, message);
            });
  }

  public boolean unknownExemplarDefinitionEntry(final Map.Entry<AstNode, TypeString> entry) {
    final TypeStringResolver resolver = this.getTypeStringResolver();
    final TypeString typeStr = entry.getValue();
    return resolver.getExemplarDefinition(typeStr) == null;
  }
}
