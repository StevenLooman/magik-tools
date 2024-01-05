package nl.ramsolutions.sw.magik;

import java.net.URI;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.DefinitionKeeperTypeKeeperAdapter;
import nl.ramsolutions.sw.magik.analysis.typing.ITypeKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.reasoner.LocalTypeReasoner;
import nl.ramsolutions.sw.magik.analysis.typing.reasoner.LocalTypeReasonerState;

/**
 * Typed magik file.
 */
public class MagikTypedFile extends MagikFile {

    private final IDefinitionKeeper definitionKeeper;
    private ITypeKeeper typeKeeper;
    private LocalTypeReasonerState reasonerState;

    /**
     * Constructor.
     * @param uri URI.
     * @param text Text.
     * @param definitionKeeper {@link IDefinitionKeeper}.
     */
    public MagikTypedFile(final URI uri, final String text, final IDefinitionKeeper definitionKeeper) {
        super(uri, text);
        this.definitionKeeper = definitionKeeper;
    }

    /**
     * Get the {@link IDefinitionKeeper}.
     */
    public IDefinitionKeeper getDefinitionKeeper() {
        return this.definitionKeeper;
    }

    /**
     * Get the {@link ITypeKeeper}.
     */
    public ITypeKeeper getTypeKeeper() {
        if (this.typeKeeper == null) {
            this.typeKeeper = new DefinitionKeeperTypeKeeperAdapter(this.definitionKeeper);
        }

        return this.typeKeeper;
    }

    /**
     * Get the resulting state from the {@link LocalTypeReasoner}.
     * @return The {@link LocalTypeReasonerState}.
     */
    public synchronized LocalTypeReasonerState getTypeReasonerState() {
        if (this.reasonerState == null) {
            final LocalTypeReasoner reasoner = new LocalTypeReasoner(this);
            reasoner.run();
            this.reasonerState = reasoner.getState();
        }

        return this.reasonerState;
    }

    @Override
    public String toString() {
        return String.format(
            "%s@%s(%s)",
            this.getClass().getName(), Integer.toHexString(this.hashCode()),
            this.getUri());
    }

}
