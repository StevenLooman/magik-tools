package nl.ramsolutions.sw.magik.analysis.typing.reasoner.restrictions;

import java.util.Collections;
import java.util.Map;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeEntry;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;

/**
 * `_isnt` type restriction.
 */
class IsntTypeRestriction extends TypeRestriction {

    private final ScopeEntry scopeEntry;
    private final AbstractType type;
    private final AbstractType restriction;

    /**
     * Constructor.
     * @param scopeEntry Scope entry.
     * @param type Type of scope entry..
     * @param restriction Restricted type.
     */
    IsntTypeRestriction(final ScopeEntry scopeEntry, final AbstractType type, final AbstractType restriction) {
        this.scopeEntry = scopeEntry;
        this.type = type;
        this.restriction = restriction;
    }

    @Override
    public TypeRestriction not() {
        return new IsTypeRestriction(this.scopeEntry, this.type, this.restriction);
    }

    @Override
    public Map<ScopeEntry, AbstractType> getRestrictions() {
        final AbstractType differenceType = AbstractType.difference(this.type, this.restriction);
        if (differenceType == null) {
            return Collections.emptyMap();
        }

        return Map.of(this.scopeEntry, differenceType);
    }

}
