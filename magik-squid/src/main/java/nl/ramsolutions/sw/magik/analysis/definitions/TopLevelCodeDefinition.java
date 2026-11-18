package nl.ramsolutions.sw.magik.analysis.definitions;

import com.sonar.sslr.api.AstNode;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import nl.ramsolutions.sw.magik.Location;

/**
 * Definition of the top-level code of one transmit unit: the code between two {@code $} (or the
 * start or end of the file) outside every method, procedure and {@code _global} statement. It holds
 * the usages no other definition holds.
 *
 * <p>Its location spans the whole unit. Its name is unique per file and module, see {@link
 * nl.ramsolutions.sw.magik.analysis.definitions.parsers.AnonymousNamer#getNameForTopLevelCode}.
 */
public class TopLevelCodeDefinition extends MagikDefinition {

  private final String name;
  private final List<MethodUsage> usedMethods;
  private final List<GlobalUsage> usedGlobals;
  private final List<ConditionUsage> usedConditions;
  private final List<BinaryOperatorUsage> usedBinaryOperators;

  /**
   * Constructor.
   *
   * @param location Location, spanning the unit.
   * @param timestamp Timestamp.
   * @param moduleName Module name.
   * @param doc Doc.
   * @param node First top-level statement of the unit outside every method.
   * @param name Name.
   * @param usedMethods Methods used, unary operators included.
   * @param usedGlobals Globals used.
   * @param usedConditions Conditions used.
   * @param usedBinaryOperators Binary operators used.
   * @param provenance Provenance.
   */
  @SuppressWarnings({"checkstyle:ParameterNumber", "java:S107"})
  public TopLevelCodeDefinition(
      final @Nullable Location location,
      final @Nullable Instant timestamp,
      final @Nullable String moduleName,
      final @Nullable String doc,
      final @Nullable AstNode node,
      final String name,
      final List<MethodUsage> usedMethods,
      final List<GlobalUsage> usedGlobals,
      final List<ConditionUsage> usedConditions,
      final List<BinaryOperatorUsage> usedBinaryOperators,
      final Provenance provenance) {
    super(location, timestamp, moduleName, doc, node, provenance);
    this.name = name;
    this.usedMethods = List.copyOf(usedMethods);
    this.usedGlobals = List.copyOf(usedGlobals);
    this.usedConditions = List.copyOf(usedConditions);
    this.usedBinaryOperators = List.copyOf(usedBinaryOperators);
  }

  @Override
  public String getName() {
    return this.name;
  }

  public List<MethodUsage> getUsedMethods() {
    return Collections.unmodifiableList(this.usedMethods);
  }

  public List<GlobalUsage> getUsedGlobals() {
    return Collections.unmodifiableList(this.usedGlobals);
  }

  public List<ConditionUsage> getUsedConditions() {
    return Collections.unmodifiableList(this.usedConditions);
  }

  public List<BinaryOperatorUsage> getUsedBinaryOperators() {
    return Collections.unmodifiableList(this.usedBinaryOperators);
  }

  @Override
  public TopLevelCodeDefinition getBareDefinition() {
    return new TopLevelCodeDefinition(
        this.getLocation(),
        this.getTimestamp(),
        this.getModuleName(),
        this.getDoc(),
        null,
        this.name,
        this.usedMethods.stream().map(MethodUsage::getWithoutNode).toList(),
        this.usedGlobals.stream().map(GlobalUsage::getWithoutNode).toList(),
        this.usedConditions.stream().map(ConditionUsage::getWithoutNode).toList(),
        this.usedBinaryOperators.stream().map(BinaryOperatorUsage::getWithoutNode).toList(),
        this.getProvenance());
  }

  @Override
  public TopLevelCodeDefinition withProvenance(final Provenance provenance) {
    return new TopLevelCodeDefinition(
        this.getLocation(),
        this.getTimestamp(),
        this.getModuleName(),
        this.getDoc(),
        this.getNode(),
        this.name,
        this.usedMethods,
        this.usedGlobals,
        this.usedConditions,
        this.usedBinaryOperators,
        provenance);
  }

  @Override
  public String toString() {
    return "%s@%s(%s)"
        .formatted(this.getClass().getName(), Integer.toHexString(this.hashCode()), this.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.getLocation(), this.getModuleName(), this.getDoc(), this.name);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }

    if (obj == null || this.getClass() != obj.getClass()) {
      return false;
    }

    final TopLevelCodeDefinition other = (TopLevelCodeDefinition) obj;
    return Objects.equals(this.getLocation(), other.getLocation())
        && Objects.equals(this.getModuleName(), other.getModuleName())
        && Objects.equals(this.getDoc(), other.getDoc())
        && Objects.equals(this.name, other.name);
  }
}
