package nl.ramsolutions.sw.magik.analysis.typing.types;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import nl.ramsolutions.sw.magik.analysis.typing.ITypeKeeper;

/** Generic definition. */
public class GenericDefinition extends AbstractType {

  private final ITypeKeeper typeKeeper;
  private final TypeString typeString;

  /**
   * Constructor.
   *
   * @param typeKeeper TypeKeeper.
   * @param typeString Defined type of generic.
   */
  public GenericDefinition(final ITypeKeeper typeKeeper, final TypeString typeString) {
    super(null, null);

    if (!typeString.isGenericDefinition()) {
      throw new IllegalArgumentException();
    }

    this.typeKeeper = typeKeeper;
    this.typeString = typeString;
  }

  public TypeString getGenericReference() {
    final String name = this.typeString.getIdentifier();
    return TypeString.ofGenericReference(name);
  }

  @Override
  public String getName() {
    return this.typeString.getFullString();
  }

  public TypeString getTypeString() {
    return this.typeString;
  }

  public AbstractType getType() {
    return this.typeKeeper.getType(this.typeString);
  }

  @Override
  public String getFullName() {
    return this.getType().getFullName();
  }

  @Override
  public Collection<Method> getMethods() {
    return this.getType().getMethods();
  }

  @Override
  public Collection<Method> getLocalMethods() {
    return this.getType().getLocalMethods();
  }

  @Override
  public Collection<AbstractType> getParents() {
    return this.getType().getParents();
  }

  @Override
  public Collection<Method> getSuperMethods(final String methodName) {
    return this.getType().getSuperMethods(methodName);
  }

  @Override
  public Collection<Method> getSuperMethods(final String methodName, final String superName) {
    return this.getType().getSuperMethods(methodName, superName);
  }

  @Override
  public Collection<Slot> getSlots() {
    return this.getType().getSlots();
  }

  @Override
  public List<GenericDefinition> getGenericDefinitions() {
    return Collections.emptyList();
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.typeString);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (obj == null) {
      return false;
    }

    if (this.getClass() != obj.getClass()) {
      return false;
    }

    final GenericDefinition other = (GenericDefinition) obj;
    return Objects.equals(this.typeString, other.typeString);
  }
}
