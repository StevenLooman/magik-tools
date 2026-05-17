package nl.ramsolutions.sw.checks.magiktyped;

import com.sonar.sslr.api.AstNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import nl.ramsolutions.sw.checks.MagikTypedCheck;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.parser.TypeDocParser;
import org.sonar.check.Rule;

/**
 * Check that variadic ({@code Type...}) tags appear only as the last entry of their kind. Applies
 * to {@code @return} and {@code @loop} — both describe 0..N values where only the last entry can be
 * variadic.
 */
@Rule(key = VariadicLastPositionTypedCheck.CHECK_KEY)
public class VariadicLastPositionTypedCheck extends MagikTypedCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "VariadicLastPosition";

  private static final String MESSAGE_RETURN =
      "Variadic @return ('Type...') must be the last @return tag.";
  private static final String MESSAGE_LOOP =
      "Variadic @loop ('Type...') must be the last @loop tag.";

  @Override
  protected void walkPostMethodDefinition(final AstNode node) {
    this.checkVariadicPosition(node);
  }

  @Override
  protected void walkPostProcedureDefinition(final AstNode node) {
    this.checkVariadicPosition(node);
  }

  private void checkVariadicPosition(final AstNode node) {
    final TypeDocParser docParser = new TypeDocParser(node);
    this.flagNonTailVariadic(docParser.getReturnTypeNodes(), MESSAGE_RETURN);
    this.flagNonTailVariadic(docParser.getLoopTypeNodes(), MESSAGE_LOOP);
  }

  private void flagNonTailVariadic(final Map<AstNode, TypeString> typeNodes, final String message) {
    final List<Map.Entry<AstNode, TypeString>> entries = new ArrayList<>(typeNodes.entrySet());
    if (entries.size() < 2) {
      return;
    }

    // All but the last entry must be non-variadic.
    for (int i = 0; i < entries.size() - 1; ++i) {
      if (entries.get(i).getValue().isVariadic()) {
        this.addIssue(entries.get(i).getKey(), message);
      }
    }
  }
}
