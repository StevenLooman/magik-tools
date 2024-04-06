package nl.ramsolutions.sw.magik.analysis.definitions;

import com.sonar.sslr.api.AstNode;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.Objects;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;

/** Parameter definition. */
public class ParameterDefinition extends Definition {

  /** Parameter modifier. */
  @SuppressWarnings("checkstyle:JavadocVariable")
  public enum Modifier {
    NONE,
    OPTIONAL,
    GATHER,
  }

  private final String name;
  private final Modifier modifier;
  private final TypeString typeName;

  /**
   * Constructor.
   *
   * @param moduleName Name of module where this is defined.
   * @param node Node of parameter.
   * @param name Name of parameter.
   * @param modifier Modifier of parameter.
   */
  public ParameterDefinition(
      final @Nullable Location location,
      final @Nullable String moduleName,
      final @Nullable String doc,
      final @Nullable AstNode node,
      final String name,
      final Modifier modifier,
      final TypeString typeName) {
    super(location, moduleName, doc, node);
    this.name = name;
    this.modifier = modifier;
    this.typeName = typeName;
  }

  public Modifier getModifier() {
    return this.modifier;
  }

  @Override
  public String getName() {
    return this.name;
  }

  public TypeString getTypeName() {
    return this.typeName;
  }

  @Override
  public String getPackage() {
    return null;
  }

  @Override
  public ParameterDefinition getWithoutNode() {
    return new ParameterDefinition(
        this.getLocation(),
        this.getModuleName(),
        this.getDoc(),
        null,
        this.name,
        this.modifier,
        this.typeName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        this.getLocation(),
        this.getModuleName(),
        this.getDoc(),
        this.name,
        this.modifier,
        this.typeName);
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

    final ParameterDefinition other = (ParameterDefinition) obj;
    return Objects.equals(other.getLocation(), this.getLocation())
        && Objects.equals(other.getName(), this.getName())
        && Objects.equals(other.getDoc(), this.getDoc())
        && Objects.equals(other.name, this.name)
        && Objects.equals(other.modifier, this.modifier)
        && Objects.equals(other.typeName, this.typeName);
  }
}
