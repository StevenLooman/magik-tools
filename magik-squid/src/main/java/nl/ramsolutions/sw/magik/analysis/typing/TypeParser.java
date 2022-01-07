package nl.ramsolutions.sw.magik.analysis.typing;

import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.CombinedType;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResult;
import nl.ramsolutions.sw.magik.analysis.typing.types.GlobalReference;
import nl.ramsolutions.sw.magik.analysis.typing.types.SelfType;
import nl.ramsolutions.sw.magik.analysis.typing.types.UndefinedType;
import nl.ramsolutions.sw.magik.api.MagikKeyword;
import nl.ramsolutions.sw.magik.api.NewDocGrammar;

/**
 * Type parser.
 */
public final class TypeParser {

    private static final String TYPE_COMBINATOR_RE = Pattern.quote(NewDocGrammar.Punctuator.TYPE_COMBINATOR.getValue());
    private static final String TYPE_SEPARATOR_RE = Pattern.quote(NewDocGrammar.Punctuator.TYPE_SEPARATOR.getValue());
    private static final String TYPE_SEPARATOR = NewDocGrammar.Punctuator.TYPE_SEPARATOR.getValue();
    private final ITypeKeeper typeKeeper;

    /**
     * Constructor.
     * @param typeKeeper TypeKeeper to get types from.
     */
    public TypeParser(final ITypeKeeper typeKeeper) {
        this.typeKeeper = typeKeeper;
    }

    /**
     * Parse a type string and return the type. The result can be a {@Link CombinedType} type when types are combined
     * with a {@code |}-sign.
     * @param typeString String to parse.
     * @param currentPakkage Package in context.
     * @return Parsed type.
     */
    public AbstractType parseTypeString(final @Nullable String typeString, final String currentPakkage) {
        if (typeString == null
            || typeString.isBlank()) {
            return UndefinedType.INSTANCE;
        }

        return Stream.of(typeString.split(TYPE_COMBINATOR_RE))
            .map(typeStr -> {
                if (typeStr.equalsIgnoreCase(SelfType.SERIALIZED_NAME)
                    || typeStr.equalsIgnoreCase(MagikKeyword.SELF.getValue())
                    || typeStr.equalsIgnoreCase(MagikKeyword.CLONE.getValue())) {
                    return SelfType.INSTANCE;
                } else if (typeStr.equalsIgnoreCase(UndefinedType.SERIALIZED_NAME)) {
                    return UndefinedType.INSTANCE;
                }

                final GlobalReference globalRef = this.getGlobalRefeference(typeStr, currentPakkage);
                return this.typeKeeper.getType(globalRef);
            })
            .reduce(CombinedType::combine)
            .orElse(UndefinedType.INSTANCE);
    }

    /**
     * Parse `identifier`.
     * @param typeString Identifier, may be prefixed with `<package>:`.
     * @param currentPakkage Current package.
     * @return Global reference.
     */
    public GlobalReference getGlobalRefeference(final String typeString, final String currentPakkage) {
        final int index = typeString.indexOf(':');
        final String pakkage = index != -1
            ? typeString.substring(0, index).trim()
            : currentPakkage;
        final String identifier = index != -1
            ? typeString.substring(index + 1).trim()
            : typeString;
        return GlobalReference.of(pakkage, identifier);
    }

    /**
     * Parse {@link ExpressionResult} from string.
     * @param expressionResultString String to parse.
     * @param currentPackage Package in context.
     * @return Parsed result.
     */
    public ExpressionResult parseExpressionResultString(
            final @Nullable String expressionResultString, final String currentPackage) {
        if (expressionResultString == null
            || expressionResultString.isBlank()
            || expressionResultString.equalsIgnoreCase(ExpressionResult.UNDEFINED_SERIALIZED_NAME)) {
            return ExpressionResult.UNDEFINED;
        }

        return Stream.of(expressionResultString.split(TYPE_SEPARATOR_RE))
            .map(typeString -> this.parseTypeString(typeString, currentPackage))
            .collect(ExpressionResult.COLLECTOR);
    }

    public static String stringifyType(final AbstractType type) {
        return type.getFullName();
    }

    public static String stringifyExpressionResult(final ExpressionResult result) {
        return result.getTypeNames(TypeParser.TYPE_SEPARATOR);
    }

}
