package nl.ramsolutions.sw.magik.analysis.scope;

import com.sonar.sslr.api.AstNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

/**
 * Scope entry.
 */
public class ScopeEntry {

    /**
     * Type of ScopeEntry.
     * Types:
     * LOCAL: _local
     * GLOBAL: _global
     * DYNAMIC: _dynamic
     * IMPORT: _import
     * RECURSIVE: _recursive
     * CONSTANT: _constant
     * PARAMETER: procedure parameter
     * DEFINITION: direct assignment
     */
    @SuppressWarnings("checkstyle:JavadocVariable")
    public enum Type {
        LOCAL,
        GLOBAL,
        DYNAMIC,
        IMPORT,
        RECURSIVE,
        CONSTANT,
        PARAMETER,

        DEFINITION;
    }

    private final Type type;
    private final String identifier;
    private final AstNode node;
    private final ScopeEntry importedEntry;
    private final List<AstNode> usages = new ArrayList<>();

    /**
     * Constructor.
     * @param type Type of entry
     * @param identifier Identifier of entry
     * @param node Node of entry
     * @param importedEntry Parent of entry, in case of an _import
     */
    public ScopeEntry(
            final Type type, final String identifier, final AstNode node, final @Nullable ScopeEntry importedEntry) {
        this.type = type;
        this.identifier = identifier;
        this.node = node;
        this.importedEntry = importedEntry;

        // Parent entry/import is usage
        if (importedEntry != null) {
            importedEntry.addUsage(node);
        }
    }

    public Type getType() {
        return this.type;
    }

    public boolean isType(final Type... isType) {
        return Stream.of(isType)
            .anyMatch(wantedType -> wantedType == this.type);
    }

    public String getIdentifier() {
        return this.identifier;
    }

    public AstNode getDefinitionNode() {
        return this.node;
    }

    @CheckForNull
    public ScopeEntry getImportedEntry() {
        return this.importedEntry;
    }

    public void addUsage(final AstNode usageNode) {
        this.usages.add(usageNode);
    }

    public List<AstNode> getUsages() {
        return Collections.unmodifiableList(this.usages);
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

        final ScopeEntry otherScopeEntry = (ScopeEntry) obj;
        return Objects.equals(otherScopeEntry.getType(), this.getType())
            && Objects.equals(otherScopeEntry.getIdentifier(), this.getIdentifier());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.identifier, this.type);
    }

    @Override
    public String toString() {
        return String.format(
            "%s@%s(%s,%s)",
            this.getClass().getName(), Integer.toHexString(this.hashCode()),
            this.getIdentifier(), this.getType());
    }

}
