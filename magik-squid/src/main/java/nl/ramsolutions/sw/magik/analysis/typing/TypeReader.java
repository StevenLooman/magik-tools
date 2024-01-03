package nl.ramsolutions.sw.magik.analysis.typing;

import javax.annotation.Nullable;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.CombinedType;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResult;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.types.GenericReference;
import nl.ramsolutions.sw.magik.analysis.typing.types.MagikType;
import nl.ramsolutions.sw.magik.analysis.typing.types.ParameterReferenceType;
import nl.ramsolutions.sw.magik.analysis.typing.types.SelfType;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.types.UndefinedType;
import nl.ramsolutions.sw.magik.parser.TypeStringParser;

/**
 * Type reader. Interprets {@link TypeString}/{@link ExpressionResultString}, and returns a
 * {@link AbstractType}/{@link ExpressionResult}, respectively.
 */
public final class TypeReader {

    private final ITypeKeeper typeKeeper;

    /**
     * Constructor.
     * @param typeKeeper TypeKeeper to get types from.
     */
    public TypeReader(final ITypeKeeper typeKeeper) {
        this.typeKeeper = typeKeeper;
    }

    /**
     * Parse a type string and return the type. The result can be a
     * {@link CombinedType} type when types are combined with a {@code |}-sign.
     * @param typeString String to parse.
     * @return Parsed type.
     */
    public AbstractType parseTypeString(final @Nullable TypeString typeString) {
        if (typeString == null) {
            return UndefinedType.INSTANCE;
        } else if (typeString.isUndefined()) {
            return UndefinedType.INSTANCE;
        } else if (typeString.isSelf()) {
            return SelfType.INSTANCE;
        } else if (typeString.isParameterReference()) {
            final String paramName = typeString.getReferencedParameter();
            return new ParameterReferenceType(paramName);
        } else if (typeString.isGenericReference()) {
            return new GenericReference(null, typeString);
        } else if (typeString.hasGenerics()) {
            final AbstractType mainType = this.typeKeeper.getType(typeString);
            if (!(mainType instanceof MagikType)) {
                return mainType;
            }

            final MagikType magikType = (MagikType) mainType;
            return new MagikType(magikType, typeString);
        } else if (typeString.isCombined()) {
            return typeString.getCombinedTypes().stream()
                .map(this::parseTypeString)
                .reduce(CombinedType::combine)
                .orElseThrow();
        }

        return this.typeKeeper.getType(typeString);
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
            .map(this::parseTypeString)
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
            .map(TypeStringParser::parseTypeString)
            .collect(ExpressionResultString.COLLECTOR);
    }

}
