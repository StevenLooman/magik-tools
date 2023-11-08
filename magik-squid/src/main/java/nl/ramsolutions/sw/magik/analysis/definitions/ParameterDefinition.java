package nl.ramsolutions.sw.magik.analysis.definitions;

import com.sonar.sslr.api.AstNode;
import javax.annotation.Nullable;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;

/**
 * Parameter definition.
 */
public class ParameterDefinition extends Definition {

    // TODO: Duplicate with nl.ramsolutions.sw.magik.analysis.typing.types.Parameter

    /**
     * Parameter modifier.
     */
    @SuppressWarnings("checkstyle:JavadocVariable")
    public enum Modifier {
        NONE,
        OPTIONAL,
        GATHER,
    }

    private final String name;
    private final Modifier modifier;
    private final TypeString typeName;

    /**
     * Constructor.
     * @param moduleName Name of module where this is defined.
     * @param node Node of parameter.
     * @param name Name of parameter.
     * @param modifier Modifier of parameter.
     */
    public ParameterDefinition(
            final @Nullable Location location,
            final @Nullable String moduleName,
            final @Nullable AstNode node,
            final String name,
            final Modifier modifier,
            final TypeString typeName,
            final String doc) {
        super(location, moduleName, node, doc);
        this.name = name;
        this.modifier = modifier;
        this.typeName = typeName;
    }

    public Modifier getModifier() {
        return this.modifier;
    }

    @Override
    public String getName() {
        return this.name;
    }

    public TypeString getTypeName() {
        return this.typeName;
    }

    @Override
    public String getPackage() {
        return null;
    }

}
