package nl.ramsolutions.sw.magik.analysis.typing.types;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import javax.annotation.Nullable;
import nl.ramsolutions.sw.magik.analysis.Location;

/**
 * Procedure instance.
 */
public class ProcedureInstance extends IntrinsicType {

    // TODO: Should this be modeled this way?
    //       Ideally a IntrinsicType + (special/named?) invoke() method, using a AliasType to bind to a variable?

    /**
     * Serializer name for anonymouse procedure.
     */
    public static final String ANONYMOUS_PROCEDURE = "__anonymous__:__procedure__";

    private final String procedureName;

    /**
     * Constructor, with loopbody types.
     * @param globalReference Global reference.
     * @param procedureName Name of method or procedure.
     */
    public ProcedureInstance(final GlobalReference globalReference, final @Nullable String procedureName) {
        super(globalReference);
        this.procedureName = procedureName != null
            ? procedureName
            : ANONYMOUS_PROCEDURE;
    }

    public String getProcedureName() {
        return this.procedureName;
    }

    @Override
    public Slot addSlot(final Location slotLocation, final String slotName) {
        throw new IllegalStateException();
    }

    @Override
    public Collection<Slot> getSlots() {
        return Collections.emptySet();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (this.getClass() != obj.getClass()) {
            return false;
        }

        final ProcedureInstance other = (ProcedureInstance) obj;
        return this == other;    // Test on identity.
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getFullName());
    }

    @Override
    public String toString() {
        return String.format(
            "%s@%s(%s)",
            this.getClass().getName(), Integer.toHexString(this.hashCode()),
            this.getFullName());
    }

}
