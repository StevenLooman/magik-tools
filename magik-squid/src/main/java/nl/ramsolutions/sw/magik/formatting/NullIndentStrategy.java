package nl.ramsolutions.sw.magik.formatting;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;

/** Null indent strategy. */
public class NullIndentStrategy extends IndentStrategy {

  public static final String NAME = "null";

  public NullIndentStrategy(final FormattingOptions options) {
    super(options);
  }

  @Override
  public String getStrategyName() {
    return NullIndentStrategy.NAME;
  }

  @Override
  public String indentFor(final Token token, final AstNode currentNode) {
    final int indentSize = token.getColumn();
    return this.indentString(indentSize);
  }
}
