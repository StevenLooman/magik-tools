package nl.ramsolutions.sw.magik.analysis.typing.reasoner.restrictions;

import java.util.Map;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeEntry;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.types.UndefinedType;

/** {@link ScopeEntry} {@link AbstractType} restriction. */
public class ScopeEntryTypeRestriction implements TypeRestriction {

  private final ScopeEntry scopeEntry;
  private final AbstractType unrestrictedType;
  private final AbstractType restrictedType;

  /**
   * Constructor.
   *
   * @param scopeEntry Scope entry.
   * @param unrestrictedType Unrestricted (current) type.
   * @param restrictedType Restricted type.
   */
  public ScopeEntryTypeRestriction(
      final ScopeEntry scopeEntry,
      final AbstractType unrestrictedType,
      final AbstractType restrictedType) {
    this.scopeEntry = scopeEntry;
    this.unrestrictedType = unrestrictedType;
    this.restrictedType = restrictedType;
  }

  /**
   * Get the inverse of ourselves.
   *
   * @return
   */
  public TypeRestriction not() {
    return new InverseScopeEntryTypeRestriction(
        this.scopeEntry, this.unrestrictedType, this.restrictedType);
  }

  /**
   * Get the restricted ScopeEntry with its reasoned type.
   *
   * @return Restricted scope entry with its reasoned type.
   */
  public Map.Entry<ScopeEntry, AbstractType> getRestriction() {
    final TypeString restrictedTypeString = this.restrictedType.getTypeString();
    // Unset/Maybe: No doubt, restricted type is a very specific type.
    // Undefined: Reasoned type of scope entry is undefined, we are narrowing it now.
    if (restrictedTypeString.equals(TypeString.SW_UNSET)
        || restrictedTypeString.equals(TypeString.SW_MAYBE)
        || this.unrestrictedType == UndefinedType.INSTANCE) {
      return Map.entry(this.scopeEntry, this.restrictedType);
    }

    final AbstractType intersectionType =
        AbstractType.intersection(this.unrestrictedType, this.restrictedType);
    if (intersectionType == null) {
      return Map.entry(this.scopeEntry, UndefinedType.INSTANCE);
    }

    return Map.entry(this.scopeEntry, intersectionType);
  }
}
