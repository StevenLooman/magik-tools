package nl.ramsolutions.sw.magik.analysis.typing.reasoner.restrictions;

import java.util.Map;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeEntry;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;

/** {@link ScopeEntry} {@link TypeString} restriction. */
public class ScopeEntryTypeRestriction implements TypeRestriction {

  private final ScopeEntry scopeEntry;
  private final TypeString unrestrictedType;
  private final TypeString restrictedType;

  /**
   * Constructor.
   *
   * @param scopeEntry Scope entry.
   * @param unrestrictedType Unrestricted (current) type.
   * @param restrictedType Restricted type.
   */
  public ScopeEntryTypeRestriction(
      final ScopeEntry scopeEntry,
      final TypeString unrestrictedType,
      final TypeString restrictedType) {
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
  public Map.Entry<ScopeEntry, TypeString> getRestriction() {
    // Unset/Maybe: No doubt, restricted type is a very specific type.
    // Undefined: Reasoned type of scope entry is undefined, we are narrowing it now.
    if (this.restrictedType.equals(TypeString.SW_UNSET)
        || this.restrictedType.equals(TypeString.SW_MAYBE)
        || this.unrestrictedType == TypeString.UNDEFINED) {
      return Map.entry(this.scopeEntry, this.restrictedType);
    }

    final TypeString intersectionType =
        TypeString.intersection(this.unrestrictedType, this.restrictedType);
    if (intersectionType == null) {
      return Map.entry(this.scopeEntry, TypeString.UNDEFINED);
    }

    return Map.entry(this.scopeEntry, intersectionType);
  }
}
