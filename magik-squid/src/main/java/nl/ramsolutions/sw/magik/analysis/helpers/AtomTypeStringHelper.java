package nl.ramsolutions.sw.magik.analysis.helpers;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.api.MagikNumberParser;

/**
 * a helper to the {@link TypeString}s for an atom, only works for simple atoms like numbers, _true,
 * etc. where there are no generics involved
 *
 * <p>The list of supported atom types can be found in the constant {@link #ATOM_TYPES}
 */
public class AtomTypeStringHelper {
  @SuppressWarnings("checkstyle:MagicNumber")
  private static final long BIGNUM_START = 1 << 29;

  // if a new simple atom type is added, add it here and in handleNode()
  public static final MagikGrammar[] ATOM_TYPES =
      new MagikGrammar[] {
        MagikGrammar.NUMBER,
        MagikGrammar.SELF,
        MagikGrammar.CLONE,
        MagikGrammar.FALSE,
        MagikGrammar.TRUE,
        MagikGrammar.MAYBE,
        MagikGrammar.UNSET,
        MagikGrammar.CHARACTER,
        MagikGrammar.REGEXP,
        MagikGrammar.STRING,
        MagikGrammar.SYMBOL,
        MagikGrammar.GLOBAL_REF,
        MagikGrammar.THISTHREAD,
        MagikGrammar.SIMPLE_VECTOR
      };

  @CheckForNull
  public static TypeString handleNode(AstNode node) {
    final AstNodeType nodeType = node.getType();
    if (!(nodeType instanceof MagikGrammar type)) {
      return TypeString.UNDEFINED;
    }

    // if a new simple atom type is added, add it here and in ATOM_TYPES
    return switch (type) {
      case NUMBER -> tNumber(node);
      case SELF -> tSelf(node);
      case CLONE -> tClone(node);
      case FALSE -> tFalse(node);
      case TRUE -> tTrue(node);
      case MAYBE -> tMaybe(node);
      case UNSET -> tUnset(node);
      case CHARACTER -> tCharacter(node);
      case REGEXP -> tRegexp(node);
      case STRING -> tString(node);
      case SYMBOL -> tSymbol(node);
      case GLOBAL_REF -> tGlobalRef(node);
      case THISTHREAD -> tThread(node);
      case SIMPLE_VECTOR -> tSimpleVector(node);
      default -> TypeString.UNDEFINED;
    };
  }

  @CheckForNull
  public static TypeString tNumber(AstNode node) {
    final String tokenValue = node.getTokenValue();
    final Number number = MagikNumberParser.parseMagikNumberSafe(tokenValue);
    if (number instanceof final Integer numberInt) {
      if (numberInt < BIGNUM_START) {
        return TypeString.SW_INTEGER;
      } else {
        return TypeString.SW_BIGNUM;
      }
    } else if (number instanceof Long) {
      return TypeString.SW_BIGNUM;
    } else if (number instanceof Double) {
      return TypeString.SW_FLOAT;
    }

    return null;
  }

  public static TypeString tSelf(AstNode node) {
    return TypeString.SELF;
  }

  public static TypeString tClone(AstNode node) {
    return tSelf(node);
  }

  public static TypeString tFalse(AstNode node) {
    return TypeString.SW_FALSE;
  }

  public static TypeString tTrue(AstNode node) {
    return tFalse(node);
  }

  public static TypeString tMaybe(AstNode node) {
    return TypeString.SW_MAYBE;
  }

  public static TypeString tUnset(AstNode node) {
    return TypeString.SW_UNSET;
  }

  public static TypeString tCharacter(AstNode node) {
    return TypeString.SW_CHARACTER;
  }

  public static TypeString tRegexp(AstNode node) {
    return TypeString.SW_SW_REGEXP;
  }

  public static TypeString tString(AstNode node) {
    return TypeString.SW_CHAR16_VECTOR_WITH_GENERICS;
  }

  public static TypeString tSymbol(AstNode node) {
    return TypeString.SW_SYMBOL;
  }

  public static TypeString tGlobalRef(AstNode node) {
    return TypeString.SW_GLOBAL_VARIABLE;
  }

  public static TypeString tThread(AstNode node) {
    return TypeString.combine(TypeString.SW_HEAVY_THREAD, TypeString.SW_LIGHT_THREAD);
  }

  public static TypeString tSimpleVector(AstNode node) {
    return TypeString.SW_SIMPLE_VECTOR;
  }
}
