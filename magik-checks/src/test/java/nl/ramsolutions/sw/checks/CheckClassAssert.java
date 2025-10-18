package nl.ramsolutions.sw.checks;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import org.assertj.core.api.AbstractAssert;
import org.sonar.check.Rule;

/** Assertions for {@link Check}s. */
public class CheckClassAssert extends AbstractAssert<CheckClassAssert, Class<? extends Check>> {

  protected CheckClassAssert(final Class<? extends Check> actual) {
    super(actual, CheckClassAssert.class);
  }

  public static CheckClassAssert assertThat(final Class<? extends Check> actual) {
    return new CheckClassAssert(actual);
  }

  /**
   * Asserts that the check class has the proper Rule annotation.
   *
   * @return This assertion object.
   */
  public CheckClassAssert hasProperRuleAnnotation() {
    this.isNotNull();

    final Rule ruleAnnotation = this.actual.getAnnotation(Rule.class);
    if (ruleAnnotation == null) {
      this.failWithMessage(
          "Expected check class <%s> to have Rule annotation, but none found",
          this.actual.getName());
    }

    final String checkName = this.getCheckName();
    final String checkKey = ruleAnnotation.key();
    if (!checkKey.equals(checkName)) {
      this.failWithMessage(
          "Expected check class <%s> to have proper check key <%s>, but was <%s>",
          this.actual.getName(), checkName, checkKey);
    }

    return this;
  }

  /**
   * Asserts that the check class has metadata.
   *
   * @return This assertion object.
   * @throws IOException -
   */
  public CheckClassAssert hasMetadata() throws IOException {
    this.isNotNull();

    final CheckMetadata metadata = this.getCheckMetadata();
    if (metadata == null) {
      this.failWithMessage(
          "Expected check class <%s> to have metadata, but none found", this.actual.getName());
    }

    return this;
  }

  /**
   * Asserts that the metadata's rule specification matches the class name.
   *
   * @return This assertion object.
   * @throws IOException -
   */
  public CheckClassAssert metadataRuleSpecificationMatchesClassName() throws IOException {
    this.isNotNull();

    final CheckMetadata metadata = this.getCheckMetadata();
    if (metadata == null) {
      this.failWithMessage(
          "Expected check class <%s> to have metadata, but none found", this.actual.getName());
    }

    final String checkName = this.getCheckName();
    final String actualRuleSpecification = metadata.getRuleSpecification();
    if (!checkName.equals(actualRuleSpecification)) {
      this.failWithMessage(
          "Expected check class <%s> to have rule specification <%s>, but was <%s>",
          this.actual.getName(), checkName, actualRuleSpecification);
    }

    return this;
  }

  /**
   * Asserts that the metadata's SQ key matches the class name in kebab-case.
   *
   * @return This assertion object.
   * @throws IOException -
   */
  public CheckClassAssert metadataSqKeyMatchesClassName() throws IOException {
    this.isNotNull();

    final CheckMetadata metadata = this.getCheckMetadata();
    if (metadata == null) {
      this.failWithMessage(
          "Expected check class <%s> to have metadata, but none found", this.actual.getName());
    }

    final String checkName = this.getCheckName();
    final String checkNameKebabCase = checkName.replaceAll("([a-z])([A-Z])", "$1-$2").toLowerCase();
    final String actualSqKey = metadata.getSqKey();
    if (!checkNameKebabCase.equals(actualSqKey)) {
      this.failWithMessage(
          "Expected check class <%s> to have SQ key <%s>, but was <%s>",
          this.actual.getName(), checkNameKebabCase, actualSqKey);
    }

    return this;
  }

  /**
   * Asserts that the check class has an associated HTML file.
   *
   * @return This assertion object.
   * @throws IOException -
   */
  public CheckClassAssert hasHtmlFile() throws IOException {
    this.isNotNull();

    final CheckHolder holder = new CheckHolder(this.actual, Collections.emptySet(), true);
    try (final InputStream htmlFileStream = holder.getHtmlFileStream()) {
      if (htmlFileStream == null) {
        this.failWithMessage("No HTML file found for check %s", this.actual.getName());
      }
    }

    return this;
  }

  private String getCheckName() {
    return this.actual.getSimpleName().replaceAll("TypedCheck$", "").replaceAll("Check$", "");
  }

  private CheckMetadata getCheckMetadata() throws IOException {
    final CheckHolder holder = new CheckHolder(this.actual, Collections.emptySet(), true);
    return holder.getMetadata();
  }
}
