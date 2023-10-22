package nl.ramsolutions.sw.magik.analysis.definitions;

import com.sonar.sslr.api.AstNode;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;

/**
 * Slotted exemplar definition.
 */
public class SlottedExemplarDefinition extends Definition {

    /**
     * Slot definition.
     */
    public static class Slot {

        private final AstNode node;
        private final String name;

        public Slot(final AstNode node, final String name) {
            this.node = node;
            this.name = name;
        }

        public AstNode getNode() {
            return node;
        }

        public String getName() {
            return name;
        }

    }

    private final List<Slot> slots;
    private final List<TypeString> parents;

    /**
     * Constructor.
     * @param moduleName Name of module where this is defined.
     * @param node Node for definition.
     * @param name Name of slotted exemplar.
     * @param slots Slots of slotted exemplar.
     * @param parents Parents of slotted exemplar.
     */
    public SlottedExemplarDefinition(
            final @Nullable String moduleName,
            final AstNode node,
            final TypeString name,
            final List<Slot> slots,
            final List<TypeString> parents) {
        super(moduleName, node, name);
        this.slots = List.copyOf(slots);
        this.parents = List.copyOf(parents);
    }

    public List<Slot> getSlots() {
        return Collections.unmodifiableList(this.slots);
    }

    public List<TypeString> getParents() {
        return Collections.unmodifiableList(this.parents);
    }

    public TypeString getTypeString() {
        return TypeString.ofIdentifier(this.getName(), this.getPackage());
    }

}
