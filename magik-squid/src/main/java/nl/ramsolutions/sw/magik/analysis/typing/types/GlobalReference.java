package nl.ramsolutions.sw.magik.analysis.typing.types;

import java.util.Objects;

/**
 * Global reference, containing package name and identifier.
 * I.e., {{@code sw:rope}.
 */
public final class GlobalReference {

    private final String pakkage;
    private final String identifier;

    /**
     * Constructor.
     * @param reference Full identifier, i.e., "sw:rope".
     */
    public GlobalReference(final String reference) {
        final String[] parts = reference.split(":");
        if (parts.length != 2) {
            throw new IllegalStateException("Malformed reference");
        }
        if (parts[0].indexOf(':') != -1
            || parts[1].indexOf(':') != -1) {
            throw new IllegalStateException("Package or identifier with package");
        }

        this.pakkage = parts[0];
        this.identifier = parts[1];
    }

    /**
     * Constructor.
     * @param pakkage Package name, i.e., "sw".
     * @param identifier Identifier, i.e., "rope".
     */
    public GlobalReference(final String pakkage, final String identifier) {
        if (pakkage.indexOf(':') != -1
            || identifier.indexOf(':') != -1) {
            throw new IllegalStateException("Malformed package or identifier");
        }

        this.pakkage = pakkage;
        this.identifier = identifier;
    }

    /**
     * Static constructor, for readability.
     * @param reference Full identifier, i.e., "sw:rope".
     * @return new GlobalReference.
     */
    public static GlobalReference of(final String reference) {
        return new GlobalReference(reference);
    }

    /**
     * Static constructor, for readability.
     * @param pakkage Package name, i.e., "sw".
     * @param identifier Identifier, i.e., "rope".
     * @return new GlobalReference.
     */
    public static GlobalReference of(final String pakkage, final String identifier) {
        return new GlobalReference(pakkage, identifier);
    }

    public String getPakkage() {
        return this.pakkage;
    }

    public String getIdentifier() {
        return this.identifier;
    }

    public String getFullName() {
        return this.pakkage + ":" + this.identifier;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.pakkage, this.identifier);
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

        final GlobalReference other = (GlobalReference) obj;
        return Objects.equals(this.pakkage, other.pakkage)
            && Objects.equals(this.identifier, other.identifier);
    }

    @Override
    public String toString() {
        return String.format(
            "%s@%s(%s:%s)",
            this.getClass().getName(), Integer.toHexString(this.hashCode()),
            this.getPakkage(), this.getIdentifier());
    }

}
