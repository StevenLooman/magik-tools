package nl.ramsolutions.sw.magik.analysis.typing.reasoner.restrictions;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeEntry;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.UndefinedType;

/**
 * Undeterminable type restriction.
 */
public class UndeterminableTypeRestriction extends TypeRestriction {

    private Set<ScopeEntry> scopeEntries;

    /**
     * Constructor.
     * @param scopeEntries Scope entries.
     */
    public UndeterminableTypeRestriction(final Set<ScopeEntry> scopeEntries) {
        this.scopeEntries = Set.copyOf(scopeEntries);
    }

    @Override
    public TypeRestriction not() {
        return this;
    }

    @Override
    public Map<ScopeEntry, AbstractType> getRestrictions() {
        return this.scopeEntries.stream()
            .map(scopeEntry -> Map.entry(scopeEntry, UndefinedType.INSTANCE))
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue));
    }

    @Override
    public boolean isDeterminable() {
        return false;
    }

}
