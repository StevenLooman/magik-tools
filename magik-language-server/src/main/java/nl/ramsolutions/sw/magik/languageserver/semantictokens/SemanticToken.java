package nl.ramsolutions.sw.magik.languageserver.semantictokens;

import com.sonar.sslr.api.Token;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/** Semantic token. */
public class SemanticToken {

  /** Semantic token type. */
  @SuppressWarnings("checkstyle:JavadocVariable")
  public enum Type {
    CLASS(0),
    PARAMETER(1),
    VARIABLE(2),
    PROPERTY(3),
    FUNCTION(4),
    METHOD(5),
    KEYWORD(6),
    MODIFIER(7),
    COMMENT(8),
    STRING(9),
    NUMBER(10),
    REGEXP(11),
    OPERATOR(12),
    TYPE_PARAMETER(13);

    private final int value;

    Type(final int value) {
      this.value = value;
    }

    /**
     * Get the name of the semantic token. This name, and its number, is communicated/matched to the
     * client.
     *
     * @return Name of semantic token.
     */
    public String getSemanticTokenName() {
      final String name = this.name().toLowerCase();
      return Pattern.compile("_([a-z])")
          .matcher(name)
          .replaceAll(match -> match.group(1).toUpperCase());
    }

    public Integer getTokenType() {
      return this.value;
    }
  }

  /** Semantic token modifier. */
  @SuppressWarnings("checkstyle:JavadocVariable")
  public enum Modifier {
    DOCUMENTATION(0x01),
    READONLY(0x02),
    VARIABLE_GLOBAL(0x04);

    private final int value;

    Modifier(final int value) {
      this.value = value;
    }

    public String getSemanticModifierName() {
      return this.name().toLowerCase();
    }

    public int getModifierType() {
      return this.value;
    }
  }

  private final Token token;
  private final Type type;
  private final Set<Modifier> modifiers;

  /**
   * Constructor.
   *
   * @param token Original token.
   * @param type Type of SemanticToken.
   * @param modifiers Modifiers for SemanticToken..
   */
  public SemanticToken(final Token token, final Type type, final Set<Modifier> modifiers) {
    this.token = token;
    this.type = type;
    this.modifiers = modifiers;
  }

  private int tokenTypeValue() {
    return this.type.getTokenType();
  }

  private int tokenModifiersValue() {
    return this.modifiers.stream()
        .mapToInt(Modifier::getModifierType)
        .reduce(0, (modifier, total) -> modifier | total);
  }

  /**
   * Get data to previous SemanticToken.
   *
   * @param previousSemanticToken Previous SemanticToken.
   * @return Data relative to previous SemanticToken.
   */
  public Stream<Integer> dataToPrevious(final SemanticToken previousSemanticToken) {
    // delta line, delta startChar, length, tokenType, tokenModifiers
    final int deltaLine = this.token.getLine() - previousSemanticToken.token.getLine();
    final int deltaStartChar =
        this.token.getLine() == previousSemanticToken.token.getLine()
            ? this.token.getColumn() - previousSemanticToken.token.getColumn()
            : this.token.getColumn();
    final int length = this.token.getOriginalValue().length();
    final int tokenTypeValue = this.tokenTypeValue();
    final int tokenModifiers = this.tokenModifiersValue();
    return Stream.of(deltaLine, deltaStartChar, length, tokenTypeValue, tokenModifiers);
  }
}
