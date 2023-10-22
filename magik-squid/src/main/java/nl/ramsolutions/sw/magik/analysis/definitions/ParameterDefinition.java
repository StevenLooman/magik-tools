package nl.ramsolutions.sw.magik.analysis.definitions;

import com.sonar.sslr.api.AstNode;
import javax.annotation.Nullable;
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

    /**
     * Constructor.
     * @param moduleName Name of module where this is defined.
     * @param node Node of parameter.
     * @param name Name of parameter.
     * @param modifier Modifier of parameter.
     */
    protected ParameterDefinition(
            final @Nullable String moduleName,
            final AstNode node,
            final String name,
            final Modifier modifier) {
        super(moduleName, node, TypeString.UNDEFINED);
        this.name = name;
        this.modifier = modifier;
    }

    public Modifier getModifier() {
        return this.modifier;
    }

    @Override
    public String getName() {
        return this.name;
    }

}
