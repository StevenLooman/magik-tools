package nl.ramsolutions.sw.magik;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import nl.ramsolutions.sw.magik.analysis.typing.ITypeKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.LocalTypeReasoner;

/**
 * Typed magik file.
 */
public class MagikTypedFile extends MagikFile {

    private final ITypeKeeper typeKeeper;
    private LocalTypeReasoner typeReasoner;

    /**
     * Constructor.
     * @param uri URI.
     * @param text Text.
     * @param typeKeeper TypeKeeper.
     */
    public MagikTypedFile(final URI uri, final String text, final ITypeKeeper typeKeeper) {
        super(uri, text);
        this.typeKeeper = typeKeeper;
    }

    /**
     * Constructor.
     * @param uri URI.
     * @param text Text.
     * @param typeKeeper TypeKeeper.
     */
    public MagikTypedFile(final String uri, final String text, final ITypeKeeper typeKeeper) {
        super(uri, text);
        this.typeKeeper = typeKeeper;
    }

    /**
     * Constructor. Read file at path.
     * @param path File to read.
     * @throws IOException -
     */
    public MagikTypedFile(final Path path, final ITypeKeeper typeKeeper) throws IOException {
        super(path);
        this.typeKeeper = typeKeeper;
    }

    /**
     * Get the {@link ITypeKeeper} used for the {@link LocalTypeReasoner}.
     */
    public ITypeKeeper getTypeKeeper() {
        return this.typeKeeper;
    }

    /**
     * Run the (cached) {@link LocalTypeReasoner} and return it.
     * @return The used {@link LocalTypeReasoner}.
     */
    public synchronized LocalTypeReasoner getTypeReasoner() {
        if (this.typeReasoner == null) {
            this.typeReasoner = new LocalTypeReasoner(this);
            this.typeReasoner.run();
        }

        return this.typeReasoner;
    }

    @Override
    public String toString() {
        return String.format(
            "%s@%s(%s)",
            this.getClass().getName(), Integer.toHexString(this.hashCode()),
            this.getUri());
    }

}
