package nl.ramsolutions.sw.magik.analysis.typing.reasoner.restrictions;

import java.util.Map;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeEntry;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;

/**
 * Restriction limiting against a constant type.
 *
 * A constant type is a type which has no instance other than self. Constant types include:
 * - `_unset`
 * - `_maybe`
 */
class IsTypeRestriction extends TypeRestriction {

    private final ScopeEntry scopeEntry;
    private final AbstractType type;
    private final AbstractType restriction;

    /**
     * Constructor.
     * @param scopeEntry Scope entry.
     * @param type Type of scope entry.
     * @param restriction Restricted type.
     */
    IsTypeRestriction(final ScopeEntry scopeEntry, final AbstractType type, final AbstractType restriction) {
        this.scopeEntry = scopeEntry;
        this.type = type;
        this.restriction = restriction;
    }

    @Override
    public TypeRestriction not() {
        return new IsntTypeRestriction(this.scopeEntry, this.type, this.restriction);
    }

    @Override
    public Map<ScopeEntry, AbstractType> getRestrictions() {
        return Map.of(this.scopeEntry, this.restriction);
    }

}
