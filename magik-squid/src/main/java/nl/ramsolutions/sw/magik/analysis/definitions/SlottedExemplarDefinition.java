package nl.ramsolutions.sw.magik.analysis.definitions;

import com.sonar.sslr.api.AstNode;
import java.util.Collections;
import java.util.List;
import nl.ramsolutions.sw.magik.analysis.typing.types.GlobalReference;

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
    private final List<String> parents;

    /**
     * Constructor.
     * @param node Node for definition.
     * @param pakkage Package defined in.
     * @param name Name of slotted exemplar.
     * @param slots Slots of slotted exemplar.
     * @param parents Parents of slotted exemplar.
     */
    public SlottedExemplarDefinition(
            final AstNode node,
            final String pakkage,
            final String name,
            final List<Slot> slots,
            final List<String> parents) {
        super(node, pakkage, name);
        this.slots = List.copyOf(slots);
        this.parents = List.copyOf(parents);
    }

    public List<Slot> getSlots() {
        return Collections.unmodifiableList(this.slots);
    }

    public List<String> getParents() {
        return Collections.unmodifiableList(this.parents);
    }

    public GlobalReference getGlobalReference() {
        return GlobalReference.of(this.getPackage(), this.getName());
    }

}
