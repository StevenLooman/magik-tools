package nl.ramsolutions.sw.magik.analysis.typing.types;

import javax.annotation.Nullable;
import nl.ramsolutions.sw.magik.api.MagikKeyword;

/**
 * Parameter.
 */
public class Parameter {

    // TODO: Duplicate with nl.ramsolutions.sw.magik.analysis.definitions.ParameterDefinition

    /**
     * Parameter modifier.
     */
    @SuppressWarnings("checkstyle:JavadocVariable")
    public enum Modifier {

        NONE(null),
        OPTIONAL(MagikKeyword.OPTIONAL),
        GATHER(MagikKeyword.GATHER);

        private final MagikKeyword keyword;

        Modifier(final @Nullable MagikKeyword keyword) {
            this.keyword = keyword;
        }

        /**
         * Get modifier value.
         * @return Modifier value.
         */
        public String getValue() {
            if (this.keyword == null) {
                return "";
            }

            return this.keyword.getValue();
        }

    }

    private final String name;
    private final Modifier modifier;
    private AbstractType type;

    /**
     * Constructor.
     * @param name Name of parameter.
     * @param modifier Parameter modifier.
     */
    public Parameter(final String name, final Modifier modifier) {
        this.name = name;
        this.modifier = modifier;
        this.type = UndefinedType.INSTANCE;
    }

    /**
     * Constructor.
     * @param name Name of parameter.
     * @param modifier Parameter modifier.
     * @param type Type.
     */
    public Parameter(final String name, final Modifier modifier, final AbstractType type) {
        this.name = name;
        this.modifier = modifier;
        this.type = type;
    }

    public String getName() {
        return this.name;
    }

    public Modifier getModifier() {
        return this.modifier;
    }

    public boolean is(Modifier isModifier) {
        return this.modifier == isModifier;
    }

    public void setType(AbstractType type) {
        this.type = type;
    }

    public AbstractType getType() {
        return this.type;
    }

    @Override
    public String toString() {
        return String.format(
                "%s@%s(%s)",
                this.getClass().getName(), Integer.toHexString(this.hashCode()),
                this.getName());
    }

}
