package nl.ramsolutions.sw.magik.analysis.typing.reasoner.restrictions;

import java.util.Map;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeEntry;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;

/**
 * `_is <value>` type restriction.
 */
class IsValueTypeRestriction extends TypeRestriction {

    private final ScopeEntry scopeEntry;
    private final AbstractType type;

    /**
     * Constructor.
     * @param scopeEntry Scope entry.
     * @param type Type of scope entry.
     */
    IsValueTypeRestriction(final ScopeEntry scopeEntry, final AbstractType type) {
        this.scopeEntry = scopeEntry;
        this.type = type;
    }

    @Override
    public TypeRestriction not() {
        return new IsntValueTypeRestriction(this.scopeEntry, this.type);
    }

    @Override
    public Map<ScopeEntry, AbstractType> getRestrictions() {
        return Map.of(this.scopeEntry, this.type);
    }

}
