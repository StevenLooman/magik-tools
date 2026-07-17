package nl.ramsolutions.sw.magik.languageserver.munit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import org.junit.jupiter.api.Test;

/** Tests for {@link MUnitTestItemProvider}. */
@SuppressWarnings("checkstyle:MagicNumber")
class MUnitTestItemProviderTest {

  private static final Location IN_FILE_LOCATION =
      new Location(MagikTypedFile.DEFAULT_URI, new Range(new Position(1, 0), new Position(1, 0)));

  private static final TypeString MY_TEST_REF = TypeString.ofIdentifier("my_test", "user");

  /** Build a keeper containing the {@code sw:test_case} base exemplar. */
  private IDefinitionKeeper buildKeeperWithTestCase() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();

    // Add sw:test_case exemplar (the base MUnit test class).
    definitionKeeper.add(
        new ExemplarDefinition(
            null,
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.SLOTTED,
            MUnitTestItemProvider.MUNIT_TEST_CASE_EXEMPLAR_NAME,
            Collections.emptyList(),
            null));

    return definitionKeeper;
  }

  /** Add a test exemplar (inheriting from {@code sw:test_case}) to the keeper. */
  private void addTestExemplar(final IDefinitionKeeper definitionKeeper) {
    definitionKeeper.add(
        new ExemplarDefinition(
            IN_FILE_LOCATION,
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.SLOTTED,
            MY_TEST_REF,
            List.of(MUnitTestItemProvider.MUNIT_TEST_CASE_EXEMPLAR_NAME),
            null));
  }

  /** Add a method with the given name to the test exemplar. */
  private void addMethod(final IDefinitionKeeper definitionKeeper, final String methodName) {
    definitionKeeper.add(
        new MethodDefinition(
            IN_FILE_LOCATION,
            null,
            null,
            null,
            null,
            MY_TEST_REF,
            methodName,
            Collections.emptySet(),
            Collections.emptyList(),
            null,
            null,
            ExpressionResultString.UNDEFINED,
            ExpressionResultString.EMPTY));
  }

  private List<String> getTestMethodNames(final IDefinitionKeeper definitionKeeper) {
    final MUnitTestItemProvider provider = new MUnitTestItemProvider(definitionKeeper);
    return provider
        .getTestMethods()
        .map(MethodDefinition::getMethodName)
        .collect(Collectors.toList());
  }

  @Test
  void testGetTestMethodsIncludesTestPrefixedMethods() {
    final IDefinitionKeeper definitionKeeper = this.buildKeeperWithTestCase();
    this.addTestExemplar(definitionKeeper);
    this.addMethod(definitionKeeper, "test_something()");
    this.addMethod(definitionKeeper, "test_another_thing()");

    final List<String> methodNames = this.getTestMethodNames(definitionKeeper);

    assertThat(methodNames).containsExactlyInAnyOrder("test_something()", "test_another_thing()");
  }

  @Test
  void testGetTestMethodsExcludesNonTestMethods() {
    final IDefinitionKeeper definitionKeeper = this.buildKeeperWithTestCase();
    this.addTestExemplar(definitionKeeper);
    this.addMethod(definitionKeeper, "test_something()");
    this.addMethod(definitionKeeper, "helper_method()");
    this.addMethod(definitionKeeper, "set_up()");

    final List<String> methodNames = this.getTestMethodNames(definitionKeeper);

    assertThat(methodNames).containsExactly("test_something()");
  }

  @Test
  void testGetTestMethodsExcludesExcludedMethodNames() {
    final IDefinitionKeeper definitionKeeper = this.buildKeeperWithTestCase();
    this.addTestExemplar(definitionKeeper);
    this.addMethod(definitionKeeper, "test_something()");
    this.addMethod(definitionKeeper, "tests()");
    this.addMethod(definitionKeeper, "test_aspects");
    this.addMethod(definitionKeeper, "test_aspects_keys");
    this.addMethod(definitionKeeper, "test_result");

    final List<String> methodNames = this.getTestMethodNames(definitionKeeper);

    assertThat(methodNames).containsExactly("test_something()");
  }

  @Test
  void testGetTestMethodsEmptyWhenNoTestCaseInKeeper() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();

    final List<String> methodNames = this.getTestMethodNames(definitionKeeper);

    assertThat(methodNames).isEmpty();
  }
}
