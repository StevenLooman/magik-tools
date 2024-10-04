package nl.ramsolutions.sw.magik.analysis.definitions;

import com.sonar.sslr.api.AstNode;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.Objects;
import nl.ramsolutions.sw.magik.Location;

/** Condition usage. */
public class ConditionUsage {

  private final String conditionName;
  private final @Nullable Location location;
  private final @Nullable AstNode node;

  /**
   * Constructor.
   *
   * @param conditionName Name of condition.
   * @param location Location of use.
   */
  public ConditionUsage(
      final String conditionName, final @Nullable Location location, final @Nullable AstNode node) {
    this.conditionName = conditionName;
    this.location = location;
    this.node = node;
  }

  /**
   * Constructor.
   *
   * @param conditionName Name of condition.
   */
  public ConditionUsage(final String conditionName) {
    this(conditionName, null, null);
  }

  public String getConditionName() {
    return this.conditionName;
  }

  @CheckForNull
  public Location getLocation() {
    return this.location;
  }

  @CheckForNull
  public AstNode getNode() {
    return this.node;
  }

  public ConditionUsage getWithoutNode() {
    return new ConditionUsage(this.conditionName, this.location, null);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.conditionName);
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

    final ConditionUsage other = (ConditionUsage) obj;
    // Location is not tested!
    return Objects.equals(other.getConditionName(), this.getConditionName());
  }
}
