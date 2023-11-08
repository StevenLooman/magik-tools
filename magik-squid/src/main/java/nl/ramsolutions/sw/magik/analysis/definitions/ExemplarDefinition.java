package nl.ramsolutions.sw.magik.analysis.definitions;

import com.sonar.sslr.api.AstNode;
import java.util.Collections;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;

/**
 * Exemplar definition.
 */
public class ExemplarDefinition extends Definition {

    /**
     * Exemplar sort.
     */
    @SuppressWarnings("checkstyle:JavadocVariable")
    public enum Sort {
        UNDEFINED,
        SLOTTED,
        INDEXED,
        INTRINSIC,
        OBJECT;
    }

    /**
     * Slot definition.
     */
    public static class Slot {

        private final Location location;
        private final AstNode node;
        private final String name;
        private final TypeString typeName;

        /**
         * Constructor.
         * @param location
         * @param node
         * @param name
         * @param typeName
         */
        public Slot(
                final @Nullable Location location,
                final @Nullable AstNode node,
                final String name,
                final TypeString typeName) {
            this.location = location;
            this.node = node;
            this.name = name;
            this.typeName = typeName;
        }

        @CheckForNull
        public Location getLocation() {
            return this.location;
        }

        @CheckForNull
        public AstNode getNode() {
            return node;
        }

        public String getName() {
            return name;
        }

        public TypeString getTypeName() {
            return this.typeName;
        }

    }

    /**
     * Generic declaration.
     */
    public static class GenericDeclaration {

        private final Location location;
        private final String name;

        public GenericDeclaration(final @Nullable Location location, final String name) {
            this.location = location;
            this.name = name;
        }

        @CheckForNull
        public Location getLocation() {
            return this.location;
        }

        public String getName() {
            return this.name;
        }

    }

    private final Sort sort;
    private final TypeString typeName;
    private final List<Slot> slots;
    private final List<TypeString> parents;
    private final List<GenericDeclaration> genericDeclarations;

    /**
     * Constructor.
     * @param moduleName Name of module where this is defined.
     * @param node Node for definition.
     * @param sort Type of exemplar.
     * @param typeName Name of slotted exemplar.
     * @param slots Slots of slotted exemplar.
     * @param parents Parents of slotted exemplar.
     */
    @SuppressWarnings("checkstyle:ParameterNumber")
    public ExemplarDefinition(
            final @Nullable Location location,
            final @Nullable String moduleName,
            final @Nullable AstNode node,
            final Sort sort,
            final TypeString typeName,
            final List<Slot> slots,
            final List<TypeString> parents,
            final String doc,
            final List<GenericDeclaration> genericDeclarations) {
        super(location, moduleName, node, doc);

        if (!typeName.isSingle()) {
            throw new IllegalStateException();
        }

        this.sort = sort;
        this.typeName = typeName;
        this.slots = List.copyOf(slots);
        this.parents = List.copyOf(parents);
        this.genericDeclarations = genericDeclarations;
    }

    public List<Slot> getSlots() {
        return Collections.unmodifiableList(this.slots);
    }

    public List<TypeString> getParents() {
        return Collections.unmodifiableList(this.parents);
    }

    public List<GenericDeclaration> getGenericDeclarations() {
        return Collections.unmodifiableList(this.genericDeclarations);
    }

    public TypeString getTypeString() {
        return this.typeName;
    }

    public Sort getSort() {
        return this.sort;
    }

    @Override
    public String getName() {
        return this.typeName.getFullString();
    }

    @Override
    public String getPackage() {
        return this.typeName.getPakkage();
    }

}
