package nl.ramsolutions.sw.magik.analysis.definitions;

import com.sonar.sslr.api.AstNode;
import javax.annotation.Nullable;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;

/**
 * Binary operator definition.
 */
public class BinaryOperatorDefinition extends Definition {

    private final String operator;
    private final TypeString lhsTypeName;
    private final TypeString rhsTypeName;
    private final TypeString resultTypeName;

    /**
     * Constructor.
     * @param moduleName Module name.
     * @param node Node for definition.
     * @param operator Operator name.
     * @param lhsTypeName Left Hand Side type.
     * @param rhsTypeName Right Hand Side type.
     * @param resultTypeName Result type.
     */
    @SuppressWarnings("checkstyle:ParameterNumber")
    public BinaryOperatorDefinition(
            final @Nullable Location location,
            final @Nullable String moduleName,
            final @Nullable AstNode node,
            final String operator,
            final TypeString lhsTypeName,
            final TypeString rhsTypeName,
            final TypeString resultTypeName,
            final String doc) {
        super(location, moduleName, node, doc);

        if (!lhsTypeName.isSingle()) {
            throw new IllegalStateException();
        }

        if (!rhsTypeName.isSingle()) {
            throw new IllegalStateException();
        }

        this.operator = operator;
        this.lhsTypeName = lhsTypeName;
        this.rhsTypeName = rhsTypeName;
        this.resultTypeName = resultTypeName;
    }

    public String getOperator() {
        return this.operator;
    }

    public TypeString getLhsTypeName() {
        return this.lhsTypeName;
    }

    public TypeString getRhsTypeName() {
        return this.rhsTypeName;
    }

    public TypeString getResultTypeName() {
        return this.resultTypeName;
    }

    @Override
    public String getName() {
        return this.lhsTypeName.getFullString() + " " + this.operator + " " + this.rhsTypeName.getFullString();
    }

    @Override
    public String getPackage() {
        return null;
    }

}
