package nl.ramsolutions.sw.magik.checks;

import java.util.List;

/** {@link MagikCheck} metadata. */
public class MagikCheckMetadata {

  /** Type. */
  @SuppressWarnings("checkstyle:JavadocVariable")
  public enum Type {
    CODE_SMELL,
    BUG;
  }

  /** Remediation. */
  public static class Remediation {

    private final String func;
    private final String constantCost;

    public Remediation(final String func, final String constantCost) {
      this.func = func;
      this.constantCost = constantCost;
    }

    public String getFunc() {
      return this.func;
    }

    public String getConstantCost() {
      return this.constantCost;
    }
  }

  private final String title;
  private final Type type;
  private final String status;
  private final Remediation remediation;
  private final List<String> tags;
  private final String defaultSeverity;
  private final String ruleSpecification;
  private final String sqKey;

  /**
   * Constructor.
   *
   * @param title Title.
   * @param type Type.
   * @param status Status.
   * @param remediation Remediation.
   * @param tags Tags.
   * @param defaultSeverity Default severity.
   * @param ruleSpecification Rule specification.
   * @param sqKey SonarQube key.
   */
  @SuppressWarnings({"checkstyle:ParameterNumber", "java:S107"})
  public MagikCheckMetadata(
      final String title,
      final Type type,
      final String status,
      final Remediation remediation,
      final List<String> tags,
      final String defaultSeverity,
      final String ruleSpecification,
      final String sqKey) {
    this.title = title;
    this.type = type;
    this.status = status;
    this.remediation = remediation;
    this.tags = List.copyOf(tags);
    this.defaultSeverity = defaultSeverity;
    this.ruleSpecification = ruleSpecification;
    this.sqKey = sqKey;
  }

  public String getTitle() {
    return this.title;
  }

  public Type getType() {
    return this.type;
  }

  public String getStatus() {
    return this.status;
  }

  public Remediation getRemediation() {
    return this.remediation;
  }

  public List<String> getTags() {
    return this.tags;
  }

  public String getDefaultSeverity() {
    return this.defaultSeverity;
  }

  public String getRuleSpecification() {
    return this.ruleSpecification;
  }

  public String getSqKey() {
    return this.sqKey;
  }
}
