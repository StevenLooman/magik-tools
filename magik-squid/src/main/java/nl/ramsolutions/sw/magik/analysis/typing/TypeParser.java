package nl.ramsolutions.sw.magik.analysis.typing;

import javax.annotation.Nullable;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.CombinedType;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResult;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.types.ParameterReferenceType;
import nl.ramsolutions.sw.magik.analysis.typing.types.SelfType;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.types.UndefinedType;

/**
 * Type parser.
 */
public final class TypeParser {

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
     * @return Parsed type.
     */
    public AbstractType parseTypeString(final @Nullable TypeString typeString) {
        if (typeString == null
            || typeString.isUndefined()) {
            return UndefinedType.INSTANCE;
        }

        return typeString.parts().stream()
            .map(typeStr -> {
                if (typeStr.isSelf()) {
                    return SelfType.INSTANCE;
                } else if (typeStr.isUndefined()) {
                    return UndefinedType.INSTANCE;
                } else if (typeStr.isParameterReference()) {
                    final String paramName = typeStr.referencedParameter();
                    return new ParameterReferenceType(paramName);
                }

                return this.typeKeeper.getType(typeStr);
            })
            .reduce(CombinedType::combine)
            .orElse(UndefinedType.INSTANCE);
    }

    /**
     * Parse {@link ExpressionResult} from {@link ExpressionResultString}.
     * @param expressionResultString {@link ExpressionResultString} to parse.
     * @return Parsed result.
     */
    public ExpressionResult parseExpressionResultString(
            final @Nullable ExpressionResultString expressionResultString) {
        if (expressionResultString == null) {
            return ExpressionResult.UNDEFINED;
        }

        return expressionResultString.stream()
            .map(typeString -> this.parseTypeString(typeString))
            .collect(ExpressionResult.COLLECTOR);
    }

    /**
     * Unparse an {@link ExpressionResult} to an {@link ExpressionResultString}.
     * @param expressionResult
     * @return Expression result string.
     */
    public static ExpressionResultString unparseExpressionResult(final ExpressionResult expressionResult) {
        return expressionResult.stream()
            .map(AbstractType::getFullName)
            .map(TypeString::new)
            .collect(ExpressionResultString.COLLECTOR);
    }

}
