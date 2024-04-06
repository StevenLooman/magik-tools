package nl.ramsolutions.sw.magik.analysis.typing.reasoner.restrictions;

import java.util.Map;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeEntry;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;

/** Inverse {@link ScopeEntry} {@link TypeString} restriction. */
public class InverseScopeEntryTypeRestriction implements TypeRestriction {

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
  public InverseScopeEntryTypeRestriction(
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
  @Override
  public TypeRestriction not() {
    return new ScopeEntryTypeRestriction(
        this.scopeEntry, this.unrestrictedType, this.restrictedType);
  }

  /**
   * Get the restricted ScopeEntry with its reasoned type.
   *
   * @return Restricted scope entry with its reasoned type.
   */
  @Override
  public Map.Entry<ScopeEntry, TypeString> getRestriction() {
    final TypeString differenceType =
        TypeString.difference(this.unrestrictedType, this.restrictedType);
    if (differenceType == null) {
      return Map.entry(this.scopeEntry, TypeString.UNDEFINED);
    }

    return Map.entry(this.scopeEntry, differenceType);
  }
}
