package nl.ramsolutions.sw.magik.analysis.typing.types;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import nl.ramsolutions.sw.magik.Location;
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

    private final Location location;
    private final String name;
    private final Modifier modifier;
    private TypeString type;

    /**
     * Constructor.
     * @param location Location.
     * @param name Name of parameter.
     * @param modifier Parameter modifier.
     */
    public Parameter(final @Nullable Location location, final String name, final Modifier modifier) {
        this.location = location;
        this.name = name;
        this.modifier = modifier;
        this.type = TypeString.UNDEFINED;
    }

    /**
     * Constructor.
     * @param location Location.
     * @param name Name of parameter.
     * @param modifier Parameter modifier.
     * @param type Type.
     */
    public Parameter(
            final @Nullable Location location,
            final String name,
            final Modifier modifier,
            final TypeString type) {
        this.location = location;
        this.name = name;
        this.modifier = modifier;
        this.type = type;
    }

    @CheckForNull
    public Location getLocation() {
        return this.location;
    }

    public String getName() {
        return this.name;
    }

    public Modifier getModifier() {
        return this.modifier;
    }

    public boolean is(Modifier... isModifiers) {
        for (final Modifier isModifier : isModifiers) {
            if (this.is(isModifier)) {
                return true;
            }
        }

        return false;
    }

    public void setType(TypeString type) {
        this.type = type;
    }

    public TypeString getType() {
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
