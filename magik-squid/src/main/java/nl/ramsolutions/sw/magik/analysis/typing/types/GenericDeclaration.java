package nl.ramsolutions.sw.magik.analysis.typing.types;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import nl.ramsolutions.sw.magik.Location;

/**
 * Generic declaration.
 */
public class GenericDeclaration extends AbstractType {

    private final Location location;
    private final String name;
    private String doc;

    /**
     * Constructor.
     * @param location Location where this generic is defined.
     * @param name Name of generic
     */
    public GenericDeclaration(final @Nullable Location location, final String name) {
        this.location = location;
        this.name = name;
    }

    @Override
    public Location getLocation() {
        return this.location;
    }

    public String getName() {
        return this.name;
    }

    /**
     * Get name a {@link TypeString}.
     * @return {@link TypeString} of self.
     */
    public TypeString getNameAsTypeString() {
        return TypeString.ofGeneric(this.name);
    }

    @Override
    public String getFullName() {
        return this.getNameAsTypeString().getIdentifier();
    }

    @Override
    public String getDoc() {
        return this.doc;
    }

    @Override
    public void setDoc(final String comment) {
        this.doc = comment;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.name);
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

        final GenericDeclaration other = (GenericDeclaration) obj;
        return Objects.equals(this.name, other.name);
    }

    @Override
    public TypeString getTypeString() {
        return TypeString.ofGeneric(this.name);
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
    public Collection<Method> getSuperMethods(String methodName) {
        return Collections.emptySet();
    }

    @Override
    public Collection<Method> getSuperMethods(String methodName, String superName) {
        return Collections.emptySet();
    }

    @Override
    public Collection<Slot> getSlots() {
        return Collections.emptySet();
    }

    @Override
    public List<GenericDeclaration> getGenerics() {
        return Collections.emptyList();
    }

}
