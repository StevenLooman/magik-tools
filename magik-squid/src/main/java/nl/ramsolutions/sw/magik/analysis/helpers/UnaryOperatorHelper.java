package nl.ramsolutions.sw.magik.analysis.helpers;

import com.sonar.sslr.api.AstNode;
import java.util.Map;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.api.MagikKeyword;
import nl.ramsolutions.sw.magik.api.MagikOperator;

/** Helper for unary operator to method-name mapping. */
public class UnaryOperatorHelper {

  private final AstNode node;

  private static final Map<String, String> UNARY_OPERATOR_METHODS =
      Map.of(
          MagikOperator.NOT.getValue(), "not",
          MagikKeyword.NOT.getValue(), "not",
          MagikOperator.MINUS.getValue(), "negated",
          MagikOperator.PLUS.getValue(), "unary_plus",
          MagikKeyword.SCATTER.getValue(), "for_scatter()");

  /**
   * Constructor.
   *
   * @param node Node to encapsulate.
   */
  public UnaryOperatorHelper(final AstNode node) {
    if (!node.is(MagikGrammar.UNARY_EXPRESSION)) {
      throw new IllegalArgumentException();
    }

    this.node = node;
  }

  /**
   * Test if encapsulated node is _allresults.
   *
   * @return {@code true} when _allresults.
   */
  public boolean isAllResults() {
    return this.node.getTokenValue().equalsIgnoreCase(MagikKeyword.ALLRESULTS.getValue());
  }

  /**
   * Test if encapsulated node is _scatter.
   *
   * @return {@code true} when _scatter.
   */
  public boolean isScatter() {
    return this.node.getTokenValue().equalsIgnoreCase(MagikKeyword.SCATTER.getValue());
  }

  /**
   * Get unary operator token as lowercase.
   *
   * @return Operator token.
   */
  public String getOperator() {
    return this.node.getTokenValue().toLowerCase();
  }

  /**
   * Get the unary operator method name for this node.
   *
   * @return Method name, or {@code null} when not a mapped unary operator.
   */
  public String getUnaryOperatorMethod() {
    return UnaryOperatorHelper.UNARY_OPERATOR_METHODS.get(this.getOperator());
  }

  /**
   * Test if encapsulated node is a mapped unary operator.
   *
   * @return {@code true} when mapped.
   */
  public boolean isUnaryOperator() {
    return this.getUnaryOperatorMethod() != null;
  }
}
