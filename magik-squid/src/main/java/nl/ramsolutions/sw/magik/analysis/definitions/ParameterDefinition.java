package nl.ramsolutions.sw.magik.analysis.definitions;

import com.sonar.sslr.api.AstNode;

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

    private final Modifier modifier;

    protected ParameterDefinition(final AstNode node, final String name, final Modifier modifier) {
        super(node, "", name);
        this.modifier = modifier;
    }

    public Modifier getModifier() {
        return this.modifier;
    }

}
