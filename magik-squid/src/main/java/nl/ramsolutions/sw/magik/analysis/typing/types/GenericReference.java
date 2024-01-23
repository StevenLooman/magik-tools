package nl.ramsolutions.sw.magik.analysis.typing.types;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import nl.ramsolutions.sw.magik.Location;

/**
 * Generic reference.
 */
public class GenericReference extends AbstractType {

    // TODO: Where do we use this?

    private final Location location;
    private final TypeString typeString;
    private String doc;

    /**
     * Constructor.
     * @param location Location where this generic is defined.
     * @param typeString Name of generic
     */
    public GenericReference(final @Nullable Location location, final TypeString typeString) {
        super(location, null);

        if (!typeString.isGenericReference()) {
            throw new IllegalArgumentException();
        }

        this.location = location;
        this.typeString = typeString;
    }

    @Override
    public Location getLocation() {
        return this.location;
    }

    @Override
    public String getName() {
        return this.typeString.getString();
    }

    @Override
    public String getFullName() {
        return this.typeString.getFullString();
    }

    @Override
    public String getDoc() {
        return this.doc;
    }

    @Override
    public void setDoc(final @Nullable String doc) {
        this.doc = doc;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.typeString);
    }

    @Override
    public TypeString getTypeString() {
        return this.typeString;
    }

    @Override
    public Collection<Method> getMethods() {
        return Collections.emptySet();
    }

    @Override
    public Collection<Method> getLocalMethods() {
        return Collections.emptySet();
    }

    @Override
    public Collection<AbstractType> getParents() {
        return Collections.emptySet();
    }

    @Override
    public Collection<Method> getSuperMethods(final String methodName) {
        return Collections.emptySet();
    }

    @Override
    public Collection<Method> getSuperMethods(final String methodName, final String superName) {
        return Collections.emptySet();
    }

    @Override
    public Collection<Slot> getSlots() {
        return Collections.emptySet();
    }

    @Override
    public List<GenericDefinition> getGenericDefinitions() {
        return Collections.emptyList();
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

        final GenericReference other = (GenericReference) obj;
        return Objects.equals(this.typeString, other.typeString);
    }

}
