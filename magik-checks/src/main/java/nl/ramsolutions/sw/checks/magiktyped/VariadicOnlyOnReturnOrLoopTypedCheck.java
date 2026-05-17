package nl.ramsolutions.sw.checks.magiktyped;

import com.sonar.sslr.api.AstNode;
import java.util.Map;
import nl.ramsolutions.sw.checks.MagikTypedCheck;
import nl.ramsolutions.sw.magik.analysis.definitions.parsers.DefSlottedExemplarParser;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.parser.TypeDocParser;
import org.sonar.check.Rule;

/**
 * Flags variadic ({@code Type...}) type-doc markers on {@code @param} or {@code @slot} tags.
 * Variadic only describes "0..N values" semantics and is meaningful on {@code @return} and
 * {@code @loop} (where the loopbody emits 0..N values per iteration). On {@code @param} or
 * {@code @slot} it is misleading or wrong.
 */
@Rule(key = VariadicOnlyOnReturnOrLoopTypedCheck.CHECK_KEY)
public class VariadicOnlyOnReturnOrLoopTypedCheck extends MagikTypedCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "VariadicOnlyOnReturnOrLoop";

  private static final String MESSAGE =
      "Variadic ('Type...') is only valid on @return or @loop; use a non-variadic type instead.";

  @Override
  protected void walkPostMethodDefinition(final AstNode node) {
    final TypeDocParser docParser = new TypeDocParser(node);
    this.checkVariadicMisuse(docParser);
  }

  @Override
  protected void walkPostProcedureDefinition(final AstNode node) {
    final TypeDocParser docParser = new TypeDocParser(node);
    this.checkVariadicMisuse(docParser);
  }

  @Override
  protected void walkPostProcedureInvocation(final AstNode node) {
    if (!DefSlottedExemplarParser.isDefSlottedExemplar(node)) {
      return;
    }
    final AstNode statementNode = node.getFirstAncestor(MagikGrammar.STATEMENT);
    final TypeDocParser docParser = new TypeDocParser(statementNode);
    this.checkVariadicMisuse(docParser);
  }

  private void checkVariadicMisuse(final TypeDocParser docParser) {
    final Map<AstNode, TypeString> parameterTypeNodes = docParser.getParameterTypeNodes();
    this.flagVariadicEntries(parameterTypeNodes);

    final Map<AstNode, TypeString> slotTypeNodes = docParser.getSlotTypeNodes();
    this.flagVariadicEntries(slotTypeNodes);
  }

  private void flagVariadicEntries(final Map<AstNode, TypeString> typeNodes) {
    typeNodes.forEach(
        (typeNode, typeString) -> {
          if (typeString.isVariadic()) {
            this.addIssue(typeNode, MESSAGE);
          }
        });
  }
}
