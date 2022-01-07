package nl.ramsolutions.sw.magik.analysis.typing.types;

import java.util.Objects;

/**
 * Slotted magik type.
 */
public class SlottedType extends MagikType {

    public SlottedType(final GlobalReference globalReference) {
        super(globalReference);
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

        final SlottedType other = (SlottedType) obj;
        return Objects.equals(this.getFullName(), other.getFullName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getFullName());
    }

}
