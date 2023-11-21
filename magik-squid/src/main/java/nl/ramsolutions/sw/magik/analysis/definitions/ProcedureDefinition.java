package nl.ramsolutions.sw.magik.analysis.definitions;

import com.sonar.sslr.api.AstNode;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;

/**
 * Procedure definition.
 */
public class ProcedureDefinition extends Definition {

    /**
     * Procedure definition modifier.
     */
    @SuppressWarnings("checkstyle:JavadocVariable")
    public enum Modifier {
        ITER,
    }

    private final Set<Modifier> modifiers;
    private final TypeString typeName;
    private final String procedureName;
    private final List<ParameterDefinition> parameters;
    private final ExpressionResultString returnTypes;
    private final ExpressionResultString loopTypes;
    private final Set<GlobalUsage> usedGlobals = new HashSet<>();
    private final Set<MethodUsage> usedMethods = new HashSet<>();
    private final Set<ConditionUsage> usedConditions = new HashSet<>();

    /**
     * Constructor.
     * @param moduleName Module name.
     * @param node Node.
     * @param modifiers Modifiers.
     * @param typeName Type name.
     * @param procedureName Procedure name.
     * @param parameters Parameters.
     * @param doc Doc.
     * @param returnTypes Return types.
     * @param loopTypes Loop types.
     */
    @SuppressWarnings("checkstyle:ParameterNumber")
    public ProcedureDefinition(
            final @Nullable Location location,
            final @Nullable String moduleName,
            final @Nullable AstNode node,
            final Set<Modifier> modifiers,
            final @Nullable TypeString typeName,
            final String procedureName,
            final List<ParameterDefinition> parameters,
            final String doc,
            final ExpressionResultString returnTypes,
            final ExpressionResultString loopTypes) {
        super(location, moduleName, node, doc);
        this.modifiers = Set.copyOf(modifiers);
        this.typeName = typeName;
        this.procedureName = procedureName;
        this.parameters = parameters;
        this.returnTypes = returnTypes;
        this.loopTypes = loopTypes;
    }

    public Set<Modifier> getModifiers() {
        return this.modifiers;
    }

    @CheckForNull
    public TypeString getTypeName() {
        return this.typeName;
    }

    public String getProcedureName() {
        return this.procedureName;
    }

    public List<ParameterDefinition> getParameters() {
        return this.parameters;
    }

    public ExpressionResultString getReturnTypes() {
        return this.returnTypes;
    }

    public ExpressionResultString getLoopTypes() {
        return this.loopTypes;
    }

    @Override
    public String getName() {
        return this.procedureName;
    }

    public Set<GlobalUsage> getUsedGlobals() {
        return Collections.unmodifiableSet(this.usedGlobals);
    }

    public Set<MethodUsage> getUsedMethods() {
        return Collections.unmodifiableSet(this.usedMethods);
    }

    public Set<ConditionUsage> getUsedConditions() {
        return Collections.unmodifiableSet(this.usedConditions);
    }

    @Override
    public String getPackage() {
        if (this.typeName != null) {
            return this.typeName.getPakkage();
        }

        return TypeString.UNDEFINED.getPakkage();
    }

}
